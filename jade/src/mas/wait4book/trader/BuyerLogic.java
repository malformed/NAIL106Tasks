package mas.wait4book.trader;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import mas.wait4book.Constants;
import mas.wait4book.onto.*;

public class BuyerLogic
{
    private TraderUtils utils;

    private Map<String, Double> max_prices;

    // private 

    public BuyerLogic(TraderUtils utils)
    {
        this.utils = utils;

        max_prices = new HashMap<String, Double>();
    }

    public double evaluateOffer(Offer offer)
    {
        double value = offer.getMoney();

        if (offer.getBooks() != null) {
            for (BookInfo bi : offer.getBooks()) {
                String name = bi.getBookName();
                if (utils.isMyGoal(name)) {
                    value += utils.goalBookValue(name);
                } else {
                    value += utils.otherBookValue(name);
                }
            }
        }

        return value;
    }

    public boolean buyBook(String book_name, double price)
    {
        Double max_value = max_prices.get(book_name);
        return price <= max_value;
    }

    public String bookToBuy()
    {
        /*
        Set<String> all_books = new HashSet<String>(Constants.getBooknames());

        all_books.removeAll(my_books);
        */

        Set<String> goal_books = utils.goalBooksSet();
        Set<String> my_books = utils.myBooksSet();

        // System.out.println("GOAL: " + goal_books.toString());
        // System.out.println("MY BOOKS: " + my_books.toString());

        goal_books.removeAll(my_books);

        Set<String> books = goal_books;

        if (books.size() == 0) {
            return null;
        }

        int rnd_book_index = utils.rnd.nextInt(books.size());

        String[] book_array = (String[])(books.toArray(new String[0]));
        String book_name = book_array[rnd_book_index];
        // System.out.println("buying: " + book_name);

        // pay at most 10% more than you value the book
        max_prices.put(book_name, Constants.getPrice(book_name) * 1.1);

        return book_name;
    }
}
