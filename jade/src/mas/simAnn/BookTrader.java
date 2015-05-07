package mas.simAnn;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetInitiator;
import jade.proto.SSContractNetResponder;
import jade.proto.SSResponderDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import mas.simAnn.onto.AgentInfo;
import mas.simAnn.onto.BookInfo;
import mas.simAnn.onto.BookOntology;
import mas.simAnn.onto.ChooseFrom;
import mas.simAnn.onto.Chosen;
import mas.simAnn.onto.GetMyInfo;
import mas.simAnn.onto.Goal;
import mas.simAnn.onto.MakeTransaction;
import mas.simAnn.onto.Offer;
import mas.simAnn.onto.SellMeBooks;
import mas.simAnn.onto.StartTrading;

/**
 * Created by Martin Pilat on 16.4.14.
 *
 * Jednoducha (testovaci) verze obchodujiciho agenta. Agent neobchoduje nijak rozumne, stara se pouze o to, aby
 * nenabizel knihy, ktere nema. (Ale stejne se muze stat, ze obcas nejakou knihu zkusi prodat dvakrat, kdyz o ni pozadaji
 * dva agenti rychle po sobe.)
 */
public class BookTrader extends Agent {

    Codec codec = new SLCodec();
    Ontology onto = BookOntology.getInstance();

    ArrayList<BookInfo> myBooks;
    ArrayList<Goal> myGoal;
    double myMoney;

    Random rnd = new Random();
    
    double annealingParameter = 1.0;
    int tick =2000;
    double steps = 3*60*1000d/tick;
    
    int currentStep = 0;
    
    private final static Logger logger = Logger.getLogger(BookTrader.class.getName());
    private final String name = this.getLocalName();
    
    
    //mapuje zajem o danou knihu na trhu
    HashMap<BookInfo, Double> demands = new HashMap<BookInfo, Double>();

    @Override
    protected void setup() {
        super.setup();

        //napred je potreba rict agentovi, jakym zpusobem jsou zpravy kodovany, a jakou pouzivame ontologii
        this.getContentManager().registerLanguage(codec);
        this.getContentManager().registerOntology(onto);

        //popis sluzby book-trader
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-trader");
        sd.setName("book-trader");

        //popis tohoto agenta a sluzeb, ktere nabizi
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(this.getAID());
        dfd.addServices(sd);
        
        //zaregistrovani s DF
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        //pridame chovani, ktere bude cekat na zpravu o zacatku obchodovani
        addBehaviour(new StartTradingBehaviour(this, MessageTemplate.MatchPerformative(ACLMessage.REQUEST)));
    }

    @Override
    protected void takeDown() {
        super.takeDown();
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // ceka na zpravu o zacatku obchodovani a potom prida obchodovaci chovani
    class StartTradingBehaviour extends AchieveREResponder {


        public StartTradingBehaviour(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {

            try {
                ContentElement ce = getContentManager().extractContent(request);

                if (!(ce instanceof Action)) {
                    throw new NotUnderstoodException("");
                }
                Action a = (Action)ce;


                //dostali jsme info, ze muzeme zacit obchodovat
                if (a.getAction() instanceof StartTrading) {

                    //zjistime si, co mame, a jake jsou nase cile
                    ACLMessage getMyInfo = new ACLMessage(ACLMessage.REQUEST);
                    getMyInfo.setLanguage(codec.getName());
                    getMyInfo.setOntology(onto.getName());

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("environment");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] envs = DFService.search(myAgent, dfd);

                    getMyInfo.addReceiver(envs[0].getName());
                    getContentManager().fillContent(getMyInfo, new Action(envs[0].getName(), new GetMyInfo()));

                    ACLMessage myInfo = FIPAService.doFipaRequestClient(myAgent, getMyInfo);

                    Result res = (Result)getContentManager().extractContent(myInfo);

                    AgentInfo ai = (AgentInfo)res.getValue();

                    myBooks = ai.getBooks();
                    myGoal = ai.getGoals();
                    myMoney = ai.getMoney();

                    Collections.sort(myGoal, goalComparator);
                    
//                    Collections.reverse(myGoal);
                    
                    //pridame chovani, ktere jednou za dve vteriny zkusi koupit vybranou knihu
                    addBehaviour(new TradingBehaviour(myAgent, tick));

                    //pridame chovani, ktere se stara o prodej knih
                    addBehaviour(new SellBook(myAgent, MessageTemplate.MatchPerformative(ACLMessage.CFP)));

                    //odpovime, ze budeme obchodovat (ta zprava se v prostredi ignoruje, ale je slusne ji poslat)
                    ACLMessage reply = request.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    return reply;
                }

                throw new NotUnderstoodException("");

            } catch (Codec.CodecException e) {
                e.printStackTrace();
            } catch (OntologyException e) {
                e.printStackTrace();
            } catch (FIPAException e) {
                e.printStackTrace();
            }

            return super.handleRequest(request);
        }

        class TradingBehaviour extends TickerBehaviour {


            public TradingBehaviour(Agent a, long period) {
                super(a, period);
            }

            @Override
            protected void onTick() {
            	currentStep += 1;
            	annealingParameter = (steps - currentStep)/steps;
                try {
                	
                    //najdeme si ostatni prodejce a pripravime zpravu
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("book-trader");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] traders = DFService.search(myAgent, dfd);

                    // vytvori zpravu SELLMEBOOKS
                    ACLMessage buyBook = new ACLMessage(ACLMessage.CFP);
                    buyBook.setLanguage(codec.getName());
                    buyBook.setOntology(onto.getName());
                    buyBook.setReplyByDate(new Date(System.currentTimeMillis()+5000));

                    // prida ostatni book traders jako prijemce zpravy
                    for (DFAgentDescription dfad : traders) {
                        if (dfad.getName().equals(myAgent.getAID()))
                            continue;
                        buyBook.addReceiver(dfad.getName());
                    }

                    for (Entry<BookInfo, Double> entry : demands.entrySet()) {
                    	demands.put(entry.getKey(), Math.max(entry.getValue() - 1, 0));
                    }
                    
                    
                    ArrayList<BookInfo> bis = new ArrayList<BookInfo>();

                    //vybereme knihu k nakupu
//                    BookInfo bi = new BookInfo();
                    
                    
                    double valuesSum = 0;
                    List<Goal> unmetGoals = getUnmetGoals();
//                    logger.log(Level.INFO, "{0} : unmet goals size : {1}", new Object[]{this.getAgent().getName(), unmetGoals.size()});
                    
                    for (Goal goal : unmetGoals) {
                    	valuesSum += goal.getValue();
                    }
                    
                    //pick two random books from our goals as to not to raise suspicion among other agents
                    // pick them based on their relative values
                    Goal goal1 = null, goal2 = null;
                    while (goal1 == null || goal2 == null) {
                    	if (goal1 != null) {
                    		if (unmetGoals.size() < 2)
                    			break;
                    	}
                    	
//                    	logger.log(Level.INFO, "{0} : valuesSum {1} {2} {3}", new Object[]{this.getAgent().getName(), valuesSum, goal1, myGoal.size()});
                    	
                    	int randomValue = rnd.nextInt((int)valuesSum);
                    		
//                    	logger.log(Level.INFO, "random Value {0}", randomValue);
                    	
                    	int lowerBound = 0, upperBound = 0;
                    	for (Goal goal : myGoal) {
                    		if (goal1 == goal)
                    			continue;
                   			upperBound += goal.getValue();
                    		
                    		if (upperBound >= randomValue && lowerBound < randomValue) {
                    			if (goal1 == null) {
                    				goal1 = goal;
                    				valuesSum -= goal1.getValue();
                    			} else if (!goal.getBook().getBookName().equals(goal1.getBook().getBookName()) ) {
                    				goal2 = goal;
                    			}
                    		}
                    		lowerBound = upperBound;
                    		
                    	}
                    	
                    }
                    
                    if (goal1 != null) {
                    	BookInfo bookInfo1 = new BookInfo();
                    	bookInfo1.setBookName(goal1.getBook().getBookName());
                    	bis.add(bookInfo1);
                    }
                    if (goal2 != null) {
                    	BookInfo bookInfo2 = new BookInfo();
                    	bookInfo2.setBookName(goal2.getBook().getBookName());
                    	bis.add(bookInfo2);
                    }

                    
                    SellMeBooks smb = new SellMeBooks();
                    smb.setBooks(bis);

                    getContentManager().fillContent(buyBook, new Action(myAgent.getAID(), smb));
                    addBehaviour(new ObtainBook(myAgent, buyBook));
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }
        }


        //vlastni chovani, ktere se stara o opratreni knihy
        class ObtainBook extends ContractNetInitiator {

            public ObtainBook(Agent a, ACLMessage cfp) {
                super(a, cfp);
            }

            Chosen c;  //musime si pamatovat, co jsme nabidli
            ArrayList<BookInfo> shouldReceive; //pamatujeme si, i co nabidl prodavajici nam


            //prodavajici nam posila nasi objednavku, zadame vlastni pozadavek na poslani platby
            @Override
            protected void handleInform(ACLMessage inform) {
                try {


                    //vytvorime informace o transakci a posleme je prostredi
                    MakeTransaction mt = new MakeTransaction();

                    mt.setSenderName(myAgent.getName());
                    mt.setReceiverName(inform.getSender().getName());
                    mt.setTradeConversationID(inform.getConversationId());

                    if (c.getOffer().getBooks() == null)
                        c.getOffer().setBooks(new ArrayList<BookInfo>());

                    mt.setSendingBooks(c.getOffer().getBooks());
                    mt.setSendingMoney(c.getOffer().getMoney());

                    if (shouldReceive == null)
                        shouldReceive = new ArrayList<BookInfo>();

                    mt.setReceivingBooks(shouldReceive);
                    mt.setReceivingMoney(0.0);

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("environment");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] envs = DFService.search(myAgent, dfd);

                    ACLMessage transReq = new ACLMessage(ACLMessage.REQUEST);
                    transReq.addReceiver(envs[0].getName());
                    transReq.setLanguage(codec.getName());
                    transReq.setOntology(onto.getName());
                    transReq.setReplyByDate(new Date(System.currentTimeMillis() + 5000));

                    getContentManager().fillContent(transReq, new Action(envs[0].getName(), mt));
                    addBehaviour(new SendBook(myAgent, transReq));

                } catch (UngroundedException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

            }

            //zpracovani nabidek od prodavajicich
            @Override
            protected void handleAllResponses(Vector responses, Vector acceptances) {

                Iterator it = responses.iterator();

                Double bestUtility = 0d;
                Offer bestOffer = null;
                ACLMessage bestResponse = null;
                //je potreba vybrat jen jednu nabidku (jinak vytvorime dve transakce se stejnym ID,
                //TODO 2015: upravit, aby bylo mozne prijmout i vice, jak pocitat ID?
//                boolean accepted = false;
                while (it.hasNext()) {
                    ACLMessage response = (ACLMessage)it.next();

                    ContentElement ce = null;
                    try {
                        if (response.getPerformative() == ACLMessage.REFUSE) {
                            continue;
                        }

                        ce = getContentManager().extractContent(response);

                        ChooseFrom cf = (ChooseFrom)ce;

                        ArrayList<Offer> offers = cf.getOffers();

                        //zjistime, ktere nabidky muzeme splnit
                        ArrayList<Offer> canFulfill = new ArrayList<Offer>();
                        for (Offer o: offers) {
                            if (o.getMoney() > myMoney)
                                continue;

                            boolean foundAll = true;
                            if (o.getBooks() != null)
                                for (BookInfo bi : o.getBooks()) {
                                    String bn = bi.getBookName();
                                    boolean found = false;
                                    for (int j = 0; j < myBooks.size(); j++) {
                                        if (myBooks.get(j).getBookName().equals(bn)) {
                                            found = true;
                                            bi.setBookID(myBooks.get(j).getBookID());
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        foundAll = false;
                                        break;
                                    }
                                }

                            if (foundAll & o.getMoney() < myMoney) {
                                canFulfill.add(o);
                            }
                        }

                        //kdyz zadnou, tak odmitneme, stejne tak, kdyz uz jsme nejakou prijali
                        if (canFulfill.size() == 0) {
                            continue;
                        }

                        
                        //vybereme nabidku
                        shouldReceive = cf.getWillSell();
                        Chosen ch = new Chosen();
                        
                        Double profits = 0d;
                        for (BookInfo book : shouldReceive) {
                        	profits += getPredictedPrice(book, true);
                        }
                        
                        for (Offer offer : canFulfill) {
                        	Double utility = profits.doubleValue();
                        	
                        	utility -= offer.getMoney();
                        	if (offer.getBooks() != null)
	                        	for (BookInfo book : offer.getBooks()) {
	                        		utility -= getPredictedPrice(book);
	                        	}
                        	
                        	if (utility > bestUtility) {
                        		bestUtility = utility;
                        		bestOffer = offer;
                        		bestResponse = response;
                        	}
                        }
 

                    } catch (Codec.CodecException e) {
                        e.printStackTrace();
                    } catch (OntologyException e) {
                        e.printStackTrace();
                    }

                }
                
                it = responses.iterator();
                
                while (it.hasNext()) {
                	ACLMessage response = (ACLMessage)it.next();
                	ACLMessage acc = response.createReply();
                	if (response == bestResponse) {
                		acc.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                		Chosen ch = new Chosen();
                		ch.setOffer(bestOffer);
                		c = ch;
                		
                		try {
							getContentManager().fillContent(acc, ch);
						} catch (CodecException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (OntologyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                	} else {
                		acc.setPerformative(ACLMessage.REJECT_PROPOSAL);
                	}
                	acceptances.add(acc);
                }

            }
        }


        //chovani, ktere se stara o prodej knih
        class SellBook extends SSResponderDispatcher {

            public SellBook(Agent a, MessageTemplate tpl) {
                super(a, tpl);
            }

            @Override
            protected Behaviour createResponder(ACLMessage initiationMsg) {
                return new SellBookResponder(myAgent, initiationMsg);
            }
        }

        class SellBookResponder extends SSContractNetResponder {

            public SellBookResponder(Agent a, ACLMessage cfp) {
                super(a, cfp);
            }

            // Zjisti ktere knihy muzeme prodat
            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {

                try {
                    Action ac = (Action)getContentManager().extractContent(cfp);

                    SellMeBooks smb = (SellMeBooks)ac.getAction();
                    ArrayList<BookInfo> books = smb.getBooks();

                    ArrayList<BookInfo> sellBooks = new ArrayList<BookInfo>();

                    //zjistime, jestli mame knihy, ktere agent chce
                    boolean found = false;
                    if (books != null) {
	                    for (int i = 0; i < books.size(); i++) {
	                    	
	                    	//zaznamenat o ktere knihy je zajem
	                    	Double value = demands.get(books.get(i));
	                    	if (value == null)
	                    		value = 0d;
	                    	demands.put(books.get(i), value+1);
	                    	
	                        for (int j = 0; j < myBooks.size(); j++) {
	                            if (myBooks.get(j).getBookName().equals(books.get(i).getBookName())) {
	                                sellBooks.add(myBooks.get(j));
	                                found = true;
	                                //break;
	                            }
	                        }
	                    }
                    } else { // if books == null
                    	
                    	logger.log(Level.INFO, "{0} : received empty SellMeBooks from {1}", new Object[]{this.getAgent().getName(), ac.getActor().getName()});
                    }

                    if (!found)
                    	throw new RefuseException("");

                    //vytvorime dve neodolatelne nabidky
                    ArrayList<Offer> offers = new ArrayList<Offer>();

//                    logger.log(Level.INFO, "{0} : co muzu prodat {1}", new Object[]{this.getAgent().getName(), sellBooks});
//                    System.out.println(this.getAgent().getName());
//                    System.out.println("co muzu prodat");
//                    for (BookInfo book : sellBooks) {
//                    	System.out.println(book);
//                    }
                    
                    Double price = 0d;
                    Offer o1 = new Offer();
                    
                    for (BookInfo book : sellBooks) {
                    	price += getPredictedPrice(book);
                    	
                    }
                    o1.setMoney(price);
                    offers.add(o1);
                    
                    { 	
                     	Goal dummyGoal = new Goal();
                     	dummyGoal.setValue(price);
                     	int position = Collections.binarySearch(myGoal, dummyGoal, goalComparator);     
                     	position = position < 0 ? -position : position;
                     	
                     	// vyhledej kterou knihu bysme mohli chtit vymenou
                     	for (int i = 0; i < Math.min(myGoal.size(), position); i++) {
							Offer o = new Offer();
							Goal goal = myGoal.get(i);
							o.setBooks(new ArrayList<BookInfo> ( Collections.singletonList( goal.getBook() ) ));
							o.setMoney( Math.max(price - getPredictedPrice(goal.getBook()), 0) );
							
							offers.add(o);
						}
                    }

                    ChooseFrom cf = new ChooseFrom();

                    cf.setWillSell(sellBooks);
                    cf.setOffers(offers);

                    //posleme nabidky
                    ACLMessage reply = cfp.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setReplyByDate(new Date(System.currentTimeMillis() + 5000));
                    getContentManager().fillContent(reply, cf);

                    
                    return reply;
                } catch (UngroundedException e) {
                    e.printStackTrace();
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                }

                throw new FailureException("");
            }
            //agent se rozhodl, ze nabidku prijme
            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {

                try {
                    ChooseFrom cf = (ChooseFrom)getContentManager().extractContent(propose);

                    //pripravime info o transakci a zadame ji prostredi
                    MakeTransaction mt = new MakeTransaction();

                    mt.setSenderName(myAgent.getName());
                    mt.setReceiverName(cfp.getSender().getName());
                    mt.setTradeConversationID(cfp.getConversationId());

                    if (cf.getWillSell() == null) {
                        cf.setWillSell(new ArrayList<BookInfo>());
                    }

                    mt.setSendingBooks(cf.getWillSell());
                    mt.setSendingMoney(0.0);

                    Chosen c = (Chosen)getContentManager().extractContent(accept);

                    if (c.getOffer().getBooks() == null) {
                        c.getOffer().setBooks(new ArrayList<BookInfo>());
                    }

                    mt.setReceivingBooks(c.getOffer().getBooks());
                    mt.setReceivingMoney(c.getOffer().getMoney());

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("environment");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] envs = DFService.search(myAgent, dfd);

                    ACLMessage transReq = new ACLMessage(ACLMessage.REQUEST);
                    transReq.addReceiver(envs[0].getName());
                    transReq.setLanguage(codec.getName());
                    transReq.setOntology(onto.getName());
                    transReq.setReplyByDate(new Date(System.currentTimeMillis() + 5000));

                    getContentManager().fillContent(transReq, new Action(envs[0].getName(), mt));

                    addBehaviour(new SendBook(myAgent, transReq));

                    ACLMessage reply = accept.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    return reply;

                } catch (UngroundedException e) {
                    e.printStackTrace();
                } catch (OntologyException e) {
                    e.printStackTrace();
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                } catch (FIPAException e) {
                    e.printStackTrace();
                }

                throw new FailureException("");
            }
        }

        //po dokonceni obchodu (prostredi poslalo info) si aktualizujeme vlastni seznam knih a cile
        class SendBook extends AchieveREInitiator {

            public SendBook(Agent a, ACLMessage msg) {
                super(a, msg);
            }

            @Override
            protected void handleInform(ACLMessage inform) {

                try {
                    ACLMessage getMyInfo = new ACLMessage(ACLMessage.REQUEST);
                    getMyInfo.setLanguage(codec.getName());
                    getMyInfo.setOntology(onto.getName());

                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("environment");
                    DFAgentDescription dfd = new DFAgentDescription();
                    dfd.addServices(sd);

                    DFAgentDescription[] envs = DFService.search(myAgent, dfd);

                    getMyInfo.addReceiver(envs[0].getName());
                    getContentManager().fillContent(getMyInfo, new Action(envs[0].getName(), new GetMyInfo()));

                    ACLMessage myInfo = FIPAService.doFipaRequestClient(myAgent, getMyInfo);

                    Result res = (Result)getContentManager().extractContent(myInfo);

                    AgentInfo ai = (AgentInfo)res.getValue();

                    myBooks = ai.getBooks();
                    myGoal = ai.getGoals();
                    myMoney = ai.getMoney();
                    
                    Collections.sort(myGoal, goalComparator);
                    
//                    Collections.reverse(myGoal);
                    
                } catch (OntologyException e) {
                    e.printStackTrace();
                } catch (FIPAException e) {
                    e.printStackTrace();
                } catch (Codec.CodecException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    
    static Comparator<Goal> goalComparator = new Comparator<Goal>(){

		public int compare(Goal o1, Goal o2) {
			// TODO Auto-generated method stub
			return (int)Math.ceil(o1.getValue() - o2.getValue());
		}
    	
    };
    
    private double getPredictedPrice(BookInfo book) {
    	return getPredictedPrice(book, false);
    }

    /**
     * Tries to guess the most probable price of a book on the market
     * @param book
     * @param buying - if <code>true</code> then price should be evaluated lower
     * @return guessed price of a book
     */
    private double getPredictedPrice(BookInfo book, boolean buying) {

    	//formula for simulated annealing
    	double scale = ( Math.log(annealingParameter/1)/Math.log(2) ) + 5;
        scale = Math.max(scale, 0.001);
        scale *= buying ? 1 : 0.5;

    	// is book in demand?
    	Double price = demands.get(book);
    	
    	price = price == null ? 0 : price;
    	
    	price += buying ? 0 : 1;
    	
    	price *= scale * 50;
    	
    	//is it my goal? => dont sell it cheap!
    	for (Goal goal : myGoal) { 
    		if (book.getBookName().equals(goal.getBook().getBookName())) {
    			price = Math.max(price, goal.getValue());
    			break;
    		}
    	}
    	
    	price = Math.max(buying ? 0 : 5, price);
//    	System.out.println("predicted price for "+book.getBookName()+" : " + price);
    	return price;
    }
    
    /**
     * Gets list of set(GOALS) - set(BOOKS)
     * @return
     */
    private List<Goal> getUnmetGoals() {
    	List<Goal> unmetGoals = new ArrayList<Goal>();
    	for (Goal goal : myGoal) {
    		boolean found = false;
    		for (BookInfo bookInfo : myBooks) {
    			if (bookInfo.getBookName().equals(goal.getBook().getBookName())) {
    				found = true;
    				break;
    			}
    		}
    		
    		if (!found)
    			unmetGoals.add(goal);
    	}
    	return unmetGoals;
    }
    
}
