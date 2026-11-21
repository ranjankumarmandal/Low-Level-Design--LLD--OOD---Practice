
import java.time.LocalDate;
import java.util.*;

class Book {
    private String isbn;
    private String title;
    private String author;
    private List<BookCopy> copies;

    public Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.copies = new ArrayList<>();
    }

    public void addCopy(BookCopy copy) {
        copies.add(copy);
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public List<BookCopy> getCopies() {
        return copies;
    }

    public List<BookCopy> getAvailableCopies() {
        List<BookCopy> available = new ArrayList<>();

        for (BookCopy copy : copies) {
            if (copy.isAvailable()) {
                available.add(copy);
            }
        }

        return available;
    }
}

class BookCopy {
    private String copyId;
    private Book book;
    private boolean available;

    public BookCopy(String copyId, Book book) {
        this.copyId = copyId;
        this.book = book;
        this.available = true;
    }

    public String getCopyId() {
        return copyId;
    }

    public Book getBook() {
        return book;
    }

    public boolean isAvailable() {
        return available;
    }

    public void borrow() {
        if (!available) {
            throw new IllegalStateException("Book copy is not available");
        }
        available = false;
    }

    public void returnCopy() {
        if (available) {
            throw new IllegalStateException("Book copy is already available");
        }
        available = true;
    }
}

class Member {
    private String memberId;
    private String name;
    private String email;

    public Member(String memberId, String name, String email) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}

class Loan {
    private String loanId;
    private Member member;
    private BookCopy bookCopy;
    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnDate;

    public Loan(String loanId, Member member, BookCopy bookCopy) {
        this.loanId = loanId;
        this.member = member;
        this.bookCopy = bookCopy;
        this.borrowDate = LocalDate.now();
        this.dueDate = borrowDate.plusDays(14);
    }

    public void completeReturn() {
        if (returnDate != null) {
            throw new IllegalStateException("Book is already returned");
        }

        returnDate = LocalDate.now();
        bookCopy.returnCopy();
    }

    public String getLoanId() {
        return loanId;
    }

    public Member getMember() {
        return member;
    }

    public BookCopy getBookCopy() {
        return bookCopy;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public boolean isActive() {
        return returnDate == null;
    }
}

class Library {
    private Map<String, Book> books;
    private Map<String, Member> members;
    private Map<String, Loan> loans;
    private int loanCounter;

    public Library() {
        books = new HashMap<>();
        members = new HashMap<>();
        loans = new HashMap<>();
        loanCounter = 1;
    }

    public void registerBook(Book book) {
        if (books.containsKey(book.getIsbn())) {
            throw new IllegalArgumentException("Book already registered");
        }

        books.put(book.getIsbn(), book);
    }

    public void registerBookCopy(String isbn, String copyId) {
        Book book = books.get(isbn);

        if (book == null) {
            throw new IllegalArgumentException("Book not found");
        }

        for (Book existingBook : books.values()) {
            for (BookCopy copy : existingBook.getCopies()) {
                if (copy.getCopyId().equals(copyId)) {
                    throw new IllegalArgumentException("Copy ID already exists");
                }
            }
        }

        book.addCopy(new BookCopy(copyId, book));
    }

    public void registerMember(Member member) {
        if (members.containsKey(member.getMemberId())) {
            throw new IllegalArgumentException("Member already registered");
        }

        members.put(member.getMemberId(), member);
    }

    public List<Book> searchBooks(String keyword) {
        List<Book> result = new ArrayList<>();
        String searchKey = keyword.toLowerCase();

        for (Book book : books.values()) {
            if (book.getTitle().toLowerCase().contains(searchKey)
                    || book.getAuthor().toLowerCase().contains(searchKey)
                    || book.getIsbn().toLowerCase().contains(searchKey)) {
                result.add(book);
            }
        }

        return result;
    }

    public Loan borrowBook(String memberId, String copyId) {
        Member member = members.get(memberId);

        if (member == null) {
            throw new IllegalArgumentException("Member not found");
        }

        BookCopy selectedCopy = findBookCopy(copyId);

        if (selectedCopy == null) {
            throw new IllegalArgumentException("Book copy not found");
        }

        if (!selectedCopy.isAvailable()) {
            throw new IllegalStateException("Book copy is not available");
        }

        selectedCopy.borrow();

        String loanId = "L" + loanCounter++;

        Loan loan = new Loan(loanId, member, selectedCopy);
        loans.put(loanId, loan);

        return loan;
    }

    public void returnBook(String loanId) {
        Loan loan = loans.get(loanId);

        if (loan == null) {
            throw new IllegalArgumentException("Loan not found");
        }

        if (!loan.isActive()) {
            throw new IllegalStateException("Book already returned");
        }

        loan.completeReturn();
    }

    public List<Loan> getMemberLoans(String memberId) {
        if (!members.containsKey(memberId)) {
            throw new IllegalArgumentException("Member not found");
        }

        List<Loan> result = new ArrayList<>();

        for (Loan loan : loans.values()) {
            if (loan.getMember().getMemberId().equals(memberId)) {
                result.add(loan);
            }
        }

        return result;
    }

    private BookCopy findBookCopy(String copyId) {
        for (Book book : books.values()) {
            for (BookCopy copy : book.getCopies()) {
                if (copy.getCopyId().equals(copyId)) {
                    return copy;
                }
            }
        }

        return null;
    }
}

public class Main {
    public static void main(String[] args) {

        Library library = new Library();

        Book book1 = new Book("ISBN101", "Clean Code", "Robert Martin");
        Book book2 = new Book("ISBN102", "Effective Java", "Joshua Bloch");

        library.registerBook(book1);
        library.registerBook(book2);

        library.registerBookCopy("ISBN101", "C101");
        library.registerBookCopy("ISBN101", "C102");
        library.registerBookCopy("ISBN102", "C103");

        Member member1 = new Member("M101", "John", "john@gmail.com");
        Member member2 = new Member("M102", "Alice", "alice@gmail.com");

        library.registerMember(member1);
        library.registerMember(member2);

        System.out.println("Searching books:");

        for (Book book : library.searchBooks("Java")) {
            System.out.println(book.getTitle() + " - " + book.getAuthor());
        }

        System.out.println("\nBorrowing book:");

        Loan loan = library.borrowBook("M101", "C101");

        System.out.println("Loan ID: " + loan.getLoanId());
        System.out.println("Member: " + loan.getMember().getName());
        System.out.println("Book: " + loan.getBookCopy().getBook().getTitle());
        System.out.println("Borrow Date: " + loan.getBorrowDate());
        System.out.println("Due Date: " + loan.getDueDate());

        System.out.println("\nReturning book:");

        library.returnBook(loan.getLoanId());

        System.out.println("Return Date: " + loan.getReturnDate());
        System.out.println("Book Available: " +
                loan.getBookCopy().isAvailable());
    }
}