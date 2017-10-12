package Books.MyBooks.Dao;

import java.util.List;

import Books.MyBooks.Model.Book;

public interface BookDao {
	int getItemCount(Filter filter);
	List<Book> getBooks(int pageStart, int pageLength, Filter filter);
	void addBook(Book book);
	void removeBook(int id);
	void updateBook(Book book);
	Book getBookById(int id);
	void makeBookRead(int id);
}
