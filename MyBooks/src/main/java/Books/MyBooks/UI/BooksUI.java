package Books.MyBooks.UI;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.components.grid.DescriptionGenerator;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.themes.ValoTheme;

import Books.MyBooks.Dao.BookDao;
import Books.MyBooks.Dao.Filter;
import Books.MyBooks.Model.Book;

@SpringUI
@Theme("mytheme")
public class BooksUI extends UI {

	private static final long serialVersionUID = 1L;
	private static final int PAGING_STEP = 10;

	@Autowired
	private BookDao bookDao;
    private final Grid<Book> grid = new Grid<>(Book.class);
    @Autowired
    private BookForm form;
    private final HorizontalLayout pagingLine = new HorizontalLayout();
    private final Filter filter = new Filter();
    private final FilterUI filterUi = new FilterUI(this);
    private final Label filterLabel = new Label("<u><font size = '4' color='red'>Filter<u>", ContentMode.HTML);
    private final HorizontalLayout filterLayout = new HorizontalLayout(filterLabel);
    private final Button markBookReadBtn = new Button("Mark read");
    private PopupView popup;
    private int currentPage = 1;
	
    @PostConstruct
	void Init()
	{
    	updateList();
	}
    
    @Override
    protected void init(VaadinRequest vaadinRequest) {
    	filterUi.setFilter(filter);
    	filterUi.setVisible(false);
    	form.setBooksUI(this);
    	form.setPrimaryStyleName(ValoTheme.FORMLAYOUT_LIGHT);
    	popup = new PopupView("", form);
    	
    	filterLayout.addLayoutClickListener(e->this.setFilterFormVisibility(true));
    	
        final VerticalLayout layout = new VerticalLayout();  
        layout.addStyleName(ValoTheme.THEME_NAME);

        grid.setColumns("title", "author", "isbn", "printYear");
        Column<Book, ?> titleColumn = grid.getColumn("title");
        titleColumn.setDescriptionGenerator(new DescriptionGenerator<Book>() {
        	@Override
			public String apply(Book t) {
				return t.getDescription();
    		}
		});
        Column<Book, String> readAlreadyCol = grid.addColumn(new ValueProvider<Book, String>() {
			@Override
			public String apply(Book source) {
				if(source.isReadAlready()) return "Yes";
				return "No";
			}
		});
        readAlreadyCol.setCaption("Read already");
        grid.addColumn(new ValueProvider<Book, String>() {
			@Override
			public String apply(Book source) {
				return "Edit";
			}
		}, new ButtonRenderer<>(event -> {
            form.setBook(event.getItem());
            popup.setPopupVisible(true);
            }));        
        grid.setSizeFull();
        
        markBookReadBtn.addClickListener(e -> {
        	Book currentBook = grid.asSingleSelect().getValue();
            if(currentBook!=null)
            {
            	if(!currentBook.isReadAlready())
            	{
            		bookDao.makeBookRead(currentBook.getId());
            		updateList();
            	}
            }
        });
        markBookReadBtn.setVisible(false);
        
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() == null) {
            	markBookReadBtn.setVisible(false);
            } else {
                markBookReadBtn.setVisible(!event.getValue().isReadAlready());
            }
        });
        
        Button addBookBtn = new Button("Add new book");
        addBookBtn.addClickListener(e -> {
            form.setBook(new Book());
            popup.setPopupVisible(true);
        });
        addBookBtn.setStyleName(ValoTheme.BUTTON_FRIENDLY);
        markBookReadBtn.setStyleName(ValoTheme.BUTTON_PRIMARY);
        
        
        HorizontalLayout buttonsLayout = new HorizontalLayout(addBookBtn, markBookReadBtn);
        
        popup.setHideOnMouseOut(false);
        
        layout.addComponents(filterUi, filterLayout, buttonsLayout, grid, pagingLine, popup);
        layout.setComponentAlignment(popup, Alignment.BOTTOM_CENTER);
        
        setContent(layout);
    }
    
    public void setFilterFormVisibility(boolean visibility)
    {
    	filterLayout.setVisible(!visibility);
    	filterUi.setVisible(visibility);
    }
    
    public void applyFilter()
    {
    	filterLabel.setValue(String.format("<u><font size = '4' color='red'>%s</u>", filter.toString()));
    	updateList();
    }

	public void updateList() {
		updateList(currentPage);
	}
	
	private void updatePagingLayout(int startPage, int lastPage)
	{
		pagingLine.removeAllComponents();
		
		int leftCornerLength = (startPage>1)?1:0;
		int rigthCornerLength = (startPage<lastPage)?1:0;
		int centerLeftEnd = startPage-1;
		int centerRightEnd = startPage+1;
		if(leftCornerLength==1)
			pagingLine.addComponent(new Button("1", new PagingClickListener(1)));
		if(centerLeftEnd>2)
		{
			Label clLabel = new Label("...");    
			pagingLine.addComponent(clLabel);
			pagingLine.setComponentAlignment(clLabel, Alignment.MIDDLE_CENTER);
		}
		if(centerLeftEnd>1)
			pagingLine.addComponent(new Button(Integer.toString(centerLeftEnd), new PagingClickListener(centerLeftEnd)));
		Label curLabel = new Label(Integer.toString(startPage)+" "); 
		pagingLine.addComponent(curLabel);
		pagingLine.setComponentAlignment(curLabel, Alignment.MIDDLE_CENTER);
		if(centerRightEnd<lastPage)
			pagingLine.addComponent(new Button(Integer.toString(centerRightEnd), new PagingClickListener(centerRightEnd)));
		if(centerRightEnd<lastPage-1)
		{
			Label crLabel = new Label("...");    
			pagingLine.addComponent(crLabel);
			pagingLine.setComponentAlignment(crLabel, Alignment.MIDDLE_CENTER);
		}
		if(rigthCornerLength==1)
			pagingLine.addComponent(new Button(Integer.toString(lastPage), new PagingClickListener(lastPage)));
	}
	
	private void updateList(int page)
	{
		int itemCount = bookDao.getItemCount(filter);
		int totalPages = itemCount/PAGING_STEP+(itemCount%PAGING_STEP==0?0:1);
		if(totalPages<page)
		{
			updateList(1);
			return;
		}
		List<Book> books = bookDao.getBooks((page-1)*PAGING_STEP, PAGING_STEP, filter);
        grid.setItems(books);
        updatePagingLayout(page, totalPages);
        currentPage = page;
	}
 
    private class PagingClickListener implements Button.ClickListener
    {
		private static final long serialVersionUID = 1L;
		private int pageNumber;
		
		public PagingClickListener(int pageNumber)
		{
			this.pageNumber = pageNumber;
		}
    	
    	@Override
		public void buttonClick(ClickEvent event) {
    		updateList(pageNumber);		
		}
    }

}
