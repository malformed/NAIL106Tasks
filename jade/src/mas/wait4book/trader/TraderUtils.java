package mas.wait4book.trader;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

import mas.wait4book.Constants;
import mas.wait4book.onto.*;

public class TraderUtils {

    private ArrayList<BookInfo> books;
    private ArrayList<Goal> goal;
    
    private BuyerLogic buyer;

    private boolean sell_goal_books = true;

    public Random rnd = new Random();

    public TraderUtils(ArrayList<BookInfo> myBooks, ArrayList<Goal> myGoal)
    {
        books = myBooks;
        goal = myGoal;

        buyer = new BuyerLogic(this);
    }

    public void dontSellGoalBooks()
    {
        sell_goal_books = false;
    }

    public BuyerLogic buyer()
    {
        return buyer;
    }

    public Set<String> goalBooksSet()
    {
        Set<String> book_set = new HashSet<String>();
        for (Goal g : goal) {
            book_set.add(g.getBook().getBookName());
        }
        return book_set;
    }

    public Set<String> myBooksSet()
    {
        Set<String> book_set = new HashSet<String>();
        for (BookInfo bi : books) {
            book_set.add(bi.getBookName());
        }
        return book_set;
    }

    public boolean isMyGoal(String book_name)
    {
        for (Goal g : goal) {
            if (g.getBook().getBookName().equals(book_name)) {
                return true;
            }
        }
        return false;
    }

    public double otherBookValue(String book_name)
    {
        double othe_book_value_modifier = 0.6;
        return Constants.getPrice(book_name) * othe_book_value_modifier;
    }

    public double goalBookValue(String book_name)
    {
        for (Goal g : goal) {
            if (g.getBook().getBookName().equals(book_name)) {
                return g.getValue();
            }
        }
        return Double.NaN;
    }

    public boolean canSpare(BookInfo book)
    {
        if (sell_goal_books) {
            return true;
        }

        boolean is_goal = false;
        for (Goal g : goal) {
            if(g.getBook().getBookName().equals(book.getBookName())) {
                is_goal = true;
                break;
            }
        }

        // System.out.printf("request for %s ", book.getBookName());
        // it is safe to sell if book isn't a goal
        if (!is_goal) {
            // System.out.println("safe-to-sell");
            return true;
        } else {
            // System.out.println("is-in-my goal list");
        }

        // otherwise check whether we have at least two
        int num_instances = 0;

        for (BookInfo bi : books) {
            if (bi.getBookName().equals(book.getBookName())) {
                ++num_instances;
            }
        }

        return num_instances > 1;
        
    }
}
