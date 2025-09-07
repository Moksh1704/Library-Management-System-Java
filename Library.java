import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Library implements Serializable {
    private ArrayList<Book> books;
    private ArrayList<User> users;
    private int nextBookId;
    private int nextUserId;

    private static final String FILE_NAME = "library_state.dat";
    private static final int BORROW_DAYS = 14;
    private static final int FINE_PER_DAY = 10; // currency units

    public Library() {
        books = new ArrayList<Book>();
        users = new ArrayList<User>();
        nextBookId = 1;
        nextUserId = 1;
    }

    // ---------- Persistence ----------
    public static Library load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            return (Library) ois.readObject();
        } catch (Exception e) {
            return new Library(); // fresh state if not found
        }
    }

    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- Users ----------
    public User addUser(String username, String password, String role) {
        User u = new User(nextUserId++, username, password, role);
        users.add(u);
        return u;
    }

    public User findUserByUsername(String username) {
        for (User u : users) if (u.getUsername().equalsIgnoreCase(username)) return u;
        return null;
    }

    public ArrayList<User> getUsers() { return users; }

    // ---------- Books ----------
    public Book addBook(String title, String author, String category) {
        Book b = new Book(nextBookId++, title, author, category);
        books.add(b);
        return b;
    }

    public boolean removeBook(int bookId) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getBookId() == bookId) {
                if (!books.get(i).isAvailable()) return false; // can't remove borrowed book
                books.remove(i);
                return true;
            }
        }
        return false;
    }

    public Book getBookById(int id) {
        for (Book b : books) if (b.getBookId() == id) return b;
        return null;
    }

    public List<Book> searchBooks(String query, String searchBy, String categoryFilter) {
        ArrayList<Book> result = new ArrayList<Book>();
        String q = query == null ? "" : query.trim().toLowerCase();
        String cat = categoryFilter == null ? "" : categoryFilter.trim().toLowerCase();
        for (Book b : books) {
            boolean matchesCategory = cat.isEmpty() || "All".equalsIgnoreCase(categoryFilter) || b.getCategory().equalsIgnoreCase(categoryFilter);
            if (!matchesCategory) continue;

            if (q.isEmpty()) {
                result.add(b);
            } else if ("Title".equalsIgnoreCase(searchBy) && b.getTitle().toLowerCase().contains(q)) {
                result.add(b);
            } else if ("Author".equalsIgnoreCase(searchBy) && b.getAuthor().toLowerCase().contains(q)) {
                result.add(b);
            } else if ("Category".equalsIgnoreCase(searchBy) && b.getCategory().toLowerCase().contains(q)) {
                result.add(b);
            } else if ("ID".equalsIgnoreCase(searchBy) && String.valueOf(b.getBookId()).contains(q)) {
                result.add(b);
            } else if ("All".equalsIgnoreCase(searchBy)
                && (b.getTitle().toLowerCase().contains(q) || b.getAuthor().toLowerCase().contains(q) || b.getCategory().toLowerCase().contains(q) || String.valueOf(b.getBookId()).contains(q))) {
                result.add(b);
            }
        }
        return result;
    }

    public ArrayList<Book> getBooks() { return books; }

    // ---------- Borrow / Return ----------
    public String borrowBook(User user, int bookId) {
        Book b = getBookById(bookId);
        if (b == null) return "Book not found.";
        if (!b.isAvailable()) return "Book is already borrowed.";
        if (!user.canBorrowMore()) return "Borrow limit reached (max " + User.BORROW_LIMIT + " books).";

        String today = LocalDate.now().toString();
        String due = LocalDate.now().plusDays(BORROW_DAYS).toString();
        b.markBorrowed(user.getUsername(), today, due);
        user.borrowBookId(bookId);
        return "Borrowed successfully. Due date: " + due;
    }

    public String returnBook(User user, int bookId) {
        Book b = getBookById(bookId);
        if (b == null) return "Book not found.";
        if (b.isAvailable()) return "Book is not borrowed.";

        // Only allow return if admin or borrower
        if (!user.isAdmin() && !user.getUsername().equalsIgnoreCase(b.getBorrowerUsername())) {
            return "You cannot return a book borrowed by another user.";
        }

        // calculate fine
        String due = b.getDueDate();
        int fine = 0;
        if (due != null) {
            try {
                LocalDate dueDate = LocalDate.parse(due);
                long daysLate = java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                if (daysLate > 0) fine = (int) (daysLate * FINE_PER_DAY);
            } catch (Exception e) {
                fine = 0;
            }
        }

        String borrower = b.getBorrowerUsername();
        b.markReturned();
        // if user returning is admin but borrower exists, remove borrow id from borrower user
        if (user.isAdmin() && borrower != null) {
            User borrowerUser = findUserByUsername(borrower);
            if (borrowerUser != null) borrowerUser.returnBookId(bookId);
        } else {
            user.returnBookId(bookId);
        }

        if (fine > 0) {
            return "Returned. Fine due: ₹" + fine;
        }
        return "Returned successfully.";
    }

    // ---------- Stats ----------
    public int totalBooks() { return books.size(); }
    public int totalBorrowed() {
        int c = 0;
        for (Book b : books) if (!b.isAvailable()) c++;
        return c;
    }
    public int totalUsers() { return users.size(); }

    // ---------- Categories ----------
    public ArrayList<String> getAllCategories() {
        ArrayList<String> cats = new ArrayList<String>();
        cats.add("All");
        for (Book b : books) {
            String c = b.getCategory() == null ? "General" : b.getCategory();
            boolean found = false;
            for (String s : cats) if (s.equalsIgnoreCase(c)) { found = true; break; }
            if (!found) cats.add(c);
        }
        return cats;
    }
}
