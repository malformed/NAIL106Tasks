package mas.wait4book.trader;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import mas.wait4book.Constants;
import mas.wait4book.onto.*;

public class BuyerLogic
{
    private TraderUtils utils;

    // private 

    public BuyerLogic(TraderUtils utils)
    {
        this.utils = utils;
    }

    public String bookToBuy()
    {
        /*
        Set<String> all_books = new HashSet<String>(Constants.getBooknames());

        all_books.removeAll(my_books);
        */

        Set<String> goal_books = utils.goalBooksSet();
        Set<String> my_books = utils.myBooksSet();

        System.out.println("GOAL: " + goal_books.toString());
        System.out.println("MY BOOKS: " + my_books.toString());

        goal_books.removeAll(my_books);

        Set<String> books = goal_books;

        if (books.size() == 0) {
            return null;
        }

        int rnd_book_index = utils.rnd.nextInt(books.size());

        String[] book_array = (String[])(books.toArray(new String[0]));
        String book_name = book_array[rnd_book_index];
        System.out.println("buying: " + book_name);
        return book_name;
    }
}
