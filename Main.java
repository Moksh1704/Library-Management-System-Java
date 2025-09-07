import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.List;

public class LibraryGUI extends JFrame {
    private Library library;
    private User currentUser;

    private JTable bookTable;
    private DefaultTableModel tableModel;

    private JTextField searchField;
    private JComboBox searchByBox;
    private JComboBox categoryBox;
    private JLabel userLabel;
    private JLabel statsLabel;
    private JButton addBtn, updateBtn, deleteBtn, borrowBtn, returnBtn, refreshBtn, logoutBtn, addUserBtn, themeToggleBtn;

    private boolean darkMode = false;

    public LibraryGUI() {
        // try to set Nimbus or fallback
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) { /* ignore */ }

        library = Library.load();

        // Seed defaults if fresh install (no users)
        if (library.getUsers().isEmpty()) {
            library.addUser("admin", "admin123", "ADMIN");
            library.addUser("user1", "1234", "USER");
            library.addBook("Clean Code", "Robert C. Martin", "Programming");
            library.addBook("Effective Java", "Joshua Bloch", "Programming");
            library.addBook("Head First Design Patterns", "Eric Freeman", "Programming");
            library.addBook("The Alchemist", "Paulo Coelho", "Fiction");
            library.save();
        }

        loginDialog(); // sets currentUser

        setTitle("Library Management System");
        setSize(1100, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        refreshCategories();
        refreshTable("");

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { library.save(); }
        });
    }

    // ---------- Login ----------
    private void loginDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int opt = JOptionPane.showConfirmDialog(null, panel, "Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            String u = usernameField.getText().trim();
            String p = new String(passwordField.getPassword());

            User found = library.findUserByUsername(u);
            if (found != null && found.checkPassword(p)) {
                currentUser = found;
            } else {
                JOptionPane.showMessageDialog(null, "Invalid credentials. Try again.");
                loginDialog();
            }
        } else {
            System.exit(0);
        }
    }

    // ---------- UI ----------
    private void initUI() {
        // Top bar
        JPanel top = new JPanel(new BorderLayout(8, 8));
        userLabel = new JLabel("Logged in as: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        userLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        userLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        top.add(userLabel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        searchField = new JTextField(20);
        String[] searchOptions = new String[] {"All", "Title", "Author", "Category", "ID"};
        searchByBox = new JComboBox(searchOptions);
        categoryBox = new JComboBox();
        JButton searchBtn = new JButton(loadIcon("icons/search.png", 18, 18));
        refreshBtn = new JButton(loadIcon("icons/refresh.png", 18, 18));
        logoutBtn = new JButton(loadIcon("icons/logout.png", 18, 18));
        addUserBtn = new JButton(loadIcon("icons/user-add.png", 18, 18));
        themeToggleBtn = new JButton("Toggle Theme");

        searchPanel.add(new JLabel("Search by:"));
        searchPanel.add(searchByBox);
        searchPanel.add(new JLabel("Category:"));
        searchPanel.add(categoryBox);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(refreshBtn);
        searchPanel.add(addUserBtn);
        searchPanel.add(themeToggleBtn);
        searchPanel.add(logoutBtn);

        top.add(searchPanel, BorderLayout.EAST);

        // Center table
        String[] cols = {"ID", "Title", "Author", "Category", "Status", "Borrower", "Borrow Date", "Due Date"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookTable = new JTable(tableModel);
        bookTable.setRowHeight(30);
        bookTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        bookTable.setDefaultRenderer(Object.class, new AvailabilityRenderer());

        JScrollPane scroll = new JScrollPane(bookTable);

        // Right dashboard
        JPanel right = new JPanel();
        right.setPreferredSize(new Dimension(220, 0));
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createTitledBorder("Dashboard"));

        statsLabel = new JLabel();
        statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateStats();

        right.add(Box.createRigidArea(new Dimension(0,10)));
        right.add(statsLabel);
        right.add(Box.createVerticalGlue());

        // Bottom buttons
        JPanel bottom = new JPanel(new GridLayout(2, 4, 10, 10));
        addBtn = new JButton("Add Book", loadIcon("icons/add.png", 18, 18));
        updateBtn = new JButton("Update Book", loadIcon("icons/edit.png", 18, 18));
        deleteBtn = new JButton("Delete Book", loadIcon("icons/delete.png", 18, 18));
        borrowBtn = new JButton("Borrow", loadIcon("icons/borrow.png", 18, 18));
        returnBtn = new JButton("Return", loadIcon("icons/return.png", 18, 18));
        JButton saveBtn = new JButton("Save", loadIcon("icons/save.png", 18, 18));
        JButton statsBtn = new JButton("Refresh Stats");

        bottom.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        bottom.add(addBtn);
        bottom.add(updateBtn);
        bottom.add(deleteBtn);
        bottom.add(borrowBtn);
        bottom.add(returnBtn);
        bottom.add(saveBtn);
        bottom.add(statsBtn);

        // Main layout
        JPanel centerWithRight = new JPanel(new BorderLayout());
        centerWithRight.add(scroll, BorderLayout.CENTER);
        centerWithRight.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(centerWithRight, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Admin controls
        setAdminControls(currentUser.isAdmin());

        // Actions
        searchBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshTable(searchField.getText().trim());
            }
        });
        refreshBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                searchField.setText("");
                refreshTable("");
            }
        });
        logoutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                library.save();
                loginDialog();
                userLabel.setText("Logged in as: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
                setAdminControls(currentUser.isAdmin());
                refreshCategories();
                refreshTable("");
            }
        });
        addUserBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { addUserDialog(); }
        });
        themeToggleBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                darkMode = !darkMode;
                updateTheme();
                refreshTable(searchField.getText().trim());
            }
        });

        addBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { addBookDialog(); } });
        updateBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { updateBookDialog(); } });
        deleteBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteSelectedBook(); } });
        borrowBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { borrowSelectedBook(); } });
        returnBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { returnSelectedBook(); } });
        saveBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { library.save(); JOptionPane.showMessageDialog(LibraryGUI.this, "Saved!"); updateStats(); } });
        statsBtn.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { updateStats(); } });

        // double-click to view details
        bookTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = bookTable.getSelectedRow();
                    if (row != -1) showBookDetails((Integer) tableModel.getValueAt(row, 0));
                }
            }
        });
    }

    private void setAdminControls(boolean isAdmin) {
        addBtn.setEnabled(isAdmin);
        updateBtn.setEnabled(isAdmin);
        deleteBtn.setEnabled(isAdmin);
        addUserBtn.setEnabled(isAdmin);
    }

    private void updateTheme() {
        if (darkMode) {
            getContentPane().setBackground(new Color(43,43,43));
            bookTable.setBackground(new Color(60,63,65));
            bookTable.setForeground(Color.WHITE);
            tableModel.fireTableDataChanged();
        } else {
            getContentPane().setBackground(null);
            bookTable.setBackground(Color.WHITE);
            bookTable.setForeground(Color.BLACK);
            tableModel.fireTableDataChanged();
        }
    }

    // ---------- Table Refresh ----------
    private void refreshTable(String query) {
        tableModel.setRowCount(0);
        String searchBy = (String) searchByBox.getSelectedItem();
        String category = (String) categoryBox.getSelectedItem();
        List<Book> list = library.searchBooks(query, searchBy, category);
        for (Book b : list) {
            tableModel.addRow(new Object[] {
                b.getBookId(),
                b.getTitle(),
                b.getAuthor(),
                b.getCategory(),
                b.isAvailable() ? "Available" : "Borrowed",
                b.getBorrowerUsername() == null ? "-" : b.getBorrowerUsername(),
                b.getBorrowDate() == null ? "-" : b.getBorrowDate(),
                b.getDueDate() == null ? "-" : b.getDueDate()
            });
        }
        updateStats();
    }

    // ---------- Dialogs & Actions ----------
    private void addUserDialog() {
        if (!currentUser.isAdmin()) return;
        JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        String[] roles = new String[] {"ADMIN", "USER"};
        JComboBox roleBox = new JComboBox(roles);

        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(new JLabel("Role:"));
        panel.add(roleBox);

        int opt = JOptionPane.showConfirmDialog(this, panel, "Add User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            String r = (String) roleBox.getSelectedItem();

            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and Password required.");
                return;
            }
            if (library.findUserByUsername(u) != null) {
                JOptionPane.showMessageDialog(this, "Username already exists.");
                return;
            }
            library.addUser(u, p, r);
            library.save();
            JOptionPane.showMessageDialog(this, "User added.");
            refreshCategories();
        }
    }

    private void addBookDialog() {
        if (!currentUser.isAdmin()) return;
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JTextField categoryField = new JTextField();
        Object[] msg = {"Title:", titleField, "Author:", authorField, "Category:", categoryField};
        int opt = JOptionPane.showConfirmDialog(this, msg, "Add Book", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String t = titleField.getText().trim();
            String a = authorField.getText().trim();
            String c = categoryField.getText().trim();
            if (t.isEmpty() || a.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title and Author required.");
                return;
            }
            library.addBook(t, a, c.isEmpty() ? "General" : c);
            library.save();
            refreshCategories();
            refreshTable(searchField.getText().trim());
        }
    }

    private void updateBookDialog() {
        if (!currentUser.isAdmin()) return;
        int row = bookTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a book first."); return; }
        int id = (Integer) tableModel.getValueAt(row, 0);
        Book b = library.getBookById(id);
        if (b == null) { JOptionPane.showMessageDialog(this, "Book not found."); return; }

        JTextField titleField = new JTextField(b.getTitle());
        JTextField authorField = new JTextField(b.getAuthor());
        JTextField categoryField = new JTextField(b.getCategory());
        Object[] msg = {"Title:", titleField, "Author:", authorField, "Category:", categoryField};
        int opt = JOptionPane.showConfirmDialog(this, msg, "Update Book", JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String t = titleField.getText().trim();
            String a = authorField.getText().trim();
            String c = categoryField.getText().trim();
            if (!t.isEmpty()) b.setTitle(t);
            if (!a.isEmpty()) b.setAuthor(a);
            if (!c.isEmpty()) b.setCategory(c);
            library.save();
            refreshCategories();
            refreshTable(searchField.getText().trim());
        }
    }

    private void deleteSelectedBook() {
        if (!currentUser.isAdmin()) return;
        int row = bookTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a book first."); return; }
        int id = (Integer) tableModel.getValueAt(row, 0);
        boolean ok = library.removeBook(id);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "Cannot delete a borrowed book.");
        } else {
            library.save();
            refreshCategories();
            refreshTable(searchField.getText().trim());
        }
    }

    private void borrowSelectedBook() {
        int row = bookTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a book first."); return; }
        int id = (Integer) tableModel.getValueAt(row, 0);
        String msg = library.borrowBook(currentUser, id);
        JOptionPane.showMessageDialog(this, msg);
        library.save();
        refreshTable(searchField.getText().trim());
    }

    private void returnSelectedBook() {
        int row = bookTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a book first."); return; }
        int id = (Integer) tableModel.getValueAt(row, 0);
        String msg = library.returnBook(currentUser, id);
        JOptionPane.showMessageDialog(this, msg);
        library.save();
        refreshTable(searchField.getText().trim());
    }

    private void showBookDetails(int id) {
        Book b = library.getBookById(id);
        if (b == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(b.getBookId()).append("\n");
        sb.append("Title: ").append(b.getTitle()).append("\n");
        sb.append("Author: ").append(b.getAuthor()).append("\n");
        sb.append("Category: ").append(b.getCategory()).append("\n");
        sb.append("Status: ").append(b.isAvailable() ? "Available" : "Borrowed").append("\n");
        sb.append("Borrower: ").append(b.getBorrowerUsername() == null ? "-" : b.getBorrowerUsername()).append("\n");
        sb.append("Borrow Date: ").append(b.getBorrowDate() == null ? "-" : b.getBorrowDate()).append("\n");
        sb.append("Due Date: ").append(b.getDueDate() == null ? "-" : b.getDueDate()).append("\n");
        JOptionPane.showMessageDialog(this, sb.toString(), "Book Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- Renderer for color-coding ----------
    private class AvailabilityRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String status = (String) table.getValueAt(row, 4); // "Available" or "Borrowed"
            if (!isSelected) {
                if ("Available".equalsIgnoreCase(status)) {
                    c.setBackground(new Color(210, 255, 210)); // light green
                } else {
                    c.setBackground(new Color(255, 230, 230)); // light red
                }
            } else {
                c.setBackground(new Color(173, 216, 230)); // selection blue
            }

            // overdue highlight in Due Date column
            if (column == 7) {
                String due = (String) table.getValueAt(row, 7);
                if (!"-".equals(due) && !isSelected) {
                    try {
                        java.time.LocalDate dueDate = java.time.LocalDate.parse(due);
                        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
                        if (days < 0) {
                            c.setBackground(new Color(255, 180, 180)); // overdue
                        } else if (days <= 3) {
                            c.setBackground(new Color(255, 230, 180)); // near due
                        }
                    } catch (Exception ex) {}
                }
            }

            if (darkMode) {
                c.setForeground(Color.WHITE);
            } else {
                c.setForeground(Color.BLACK);
            }

            return c;
        }
    }

    // ---------- Categories & Stats ----------
    private void refreshCategories() {
        categoryBox.removeAllItems();
        java.util.ArrayList<String> cats = library.getAllCategories();
        for (String c : cats) categoryBox.addItem(c);
    }

    private void updateStats() {
        String s = "<html><body style='width:200px;padding:6px;font-family:SansSerif'>";
        s += "<b>Total Books:</b> " + library.totalBooks() + "<br>";
        s += "<b>Borrowed:</b> " + library.totalBorrowed() + "<br>";
        s += "<b>Total Users:</b> " + library.totalUsers() + "<br>";
        s += "<b>Your borrowed:</b> " + currentUser.borrowedCount() + "<br>";
        s += "<b>Borrow limit:</b> " + User.BORROW_LIMIT;
        s += "</body></html>";
        statsLabel.setText(s);
    }

    // ---------- Helper to load icons (optional) ----------
    private ImageIcon loadIcon(String path, int w, int h) {
        try {
            java.net.URL url = getClass().getResource("/" + path);
            if (url == null) {
                // try file fallback
                java.io.File f = new java.io.File(path);
                if (f.exists()) url = f.toURI().toURL();
            }
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
        } catch (Exception e) { /* ignore, return null below */ }
        return null;
    }
}
