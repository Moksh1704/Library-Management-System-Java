import java.io.Serializable;

public class Book implements Serializable {
    private int bookId;
    private String title;
    private String author;
    private String category;
    private boolean available;
    private String borrowerUsername;   // null if available
    private String borrowDate;         // YYYY-MM-DD or null
    private String dueDate;            // YYYY-MM-DD or null

    public Book(int bookId, String title, String author, String category) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.category = category == null ? "General" : category;
        this.available = true;
        this.borrowerUsername = null;
        this.borrowDate = null;
        this.dueDate = null;
    }

    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public boolean isAvailable() { return available; }
    public String getBorrowerUsername() { return borrowerUsername; }
    public String getBorrowDate() { return borrowDate; }
    public String getDueDate() { return dueDate; }

    public void setTitle(String title) { this.title = title; }
    public void setAuthor(String author) { this.author = author; }
    public void setCategory(String category) { this.category = category; }

    public void markBorrowed(String username, String date, String dueDate) {
        this.available = false;
        this.borrowerUsername = username;
        this.borrowDate = date;
        this.dueDate = dueDate;
    }

    public void markReturned() {
        this.available = true;
        this.borrowerUsername = null;
        this.borrowDate = null;
        this.dueDate = null;
    }
}
