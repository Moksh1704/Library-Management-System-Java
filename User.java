import java.io.Serializable;
import java.util.ArrayList;

public class User implements Serializable {
    private int userId;
    private String username;
    private String password;
    private String role; // "ADMIN" or "USER"
    private ArrayList<Integer> borrowedBookIds;

    public static final int BORROW_LIMIT = 5;

    public User(int userId, String username, String password, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.borrowedBookIds = new ArrayList<Integer>();
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public ArrayList<Integer> getBorrowedBookIds() { return borrowedBookIds; }

    public boolean checkPassword(String input) { return password.equals(input); }
    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
    public int borrowedCount() { return borrowedBookIds.size(); }

    public boolean canBorrowMore() {
        return borrowedBookIds.size() < BORROW_LIMIT;
    }

    public void borrowBookId(int id) {
        if (!borrowedBookIds.contains(id)) borrowedBookIds.add(id);
    }

    public void returnBookId(int id) {
        borrowedBookIds.remove(Integer.valueOf(id));
    }
}
