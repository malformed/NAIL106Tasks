package mas.wait4book.trader;

import java.util.Random;
import java.util.ArrayList;

import mas.wait4book.Constants;
import mas.wait4book.onto.*;

public class OfferMaker {

    private ArrayList<BookInfo> books;
    private ArrayList<Goal> goal;
    private Random rnd = new Random();

    public OfferMaker(ArrayList<BookInfo> myBooks, ArrayList<Goal> myGoal)
    {
        books = myBooks;
        goal = myGoal;
    }

    public ArrayList<Offer> makeOffers(ArrayList<BookInfo> requestBooks)
    {
        ArrayList<Offer> offers = new ArrayList<Offer>();

        // first offer is just dafualt books value + 20%
        double booksValue = defaultValue(requestBooks) * 1.2;
        Offer o1 = new Offer();
        o1.setMoney(booksValue);

        // second offer requests a needed book 
        BookInfo book = goal.get(rnd.nextInt(goal.size())).getBook();
        double mybookValue = valueOfGoalBook(book);
        double diffValue = 0.0;

        if (mybookValue > booksValue) {
            diffValue = (mybookValue - booksValue) * 1.1;
        }

        ArrayList<BookInfo> bis = new ArrayList<BookInfo>();
        bis.add(book);

        Offer o2 = new Offer();
        o2.setBooks(bis);
        o2.setMoney(diffValue);
        
        offers.add(o1);
        offers.add(o2);

        // System.out.println(o1.toString());
        // System.out.println(o2.toString());

        return offers;
    }

    private double defaultValue(ArrayList<BookInfo> books)
    {
        double value = 0.0;
        for (BookInfo book : books) {
            value += Constants.getPrice(book.getBookName());
        }
        return value;
    }

    private double valueOfGoalBook(BookInfo book)
    {
        for (Goal g : goal) {
            if (g.getBook().getBookName().equals(book.getBookName())) {
                return g.getValue();
            }
        }
        return Double.NaN;
    }
}
