package georg;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Servlet implementation class Fritz
 */
public class Fritz extends HttpServlet implements HttpSessionListener {

	private static final long serialVersionUID = 1L;

	public static final String www_path = KonfigFiles.getString(KonfigFiles.FRITZ_WWW_PATH);
	public static final String tmp_path = KonfigFiles.getString(KonfigFiles.FRITZ_TMP_PATH);
	public static final String root_path = KonfigFiles.getString(KonfigFiles.FRITZ_ROOT_PATH);
	public static final String drop_path = KonfigFiles.getString(KonfigFiles.FRITZ_DROP_PATH);;
	public static final String drop_user = KonfigFiles.getString(KonfigFiles.FRITZ_DROP_USER);;
	public static final String drop_pw = KonfigFiles.getString(KonfigFiles.FRITZ_DROP_PW);;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Fritz() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameter("id") != null) {
			if (request.getParameter("id").equals(88)) {
				Box box = new Box(request);
				box.createStreamList_m3u(box.new DropList().getList(),"list");
			} else
				new Box(request).new DropDownload(request.getParameter("id")).download(response);
		} else if (request.getParameter("stream") != null) {
			new Box(request).new DropStream().stream(response,request.getSession(false).getId());
		} else {
			request.setAttribute("index.jsp", "index.jsp");
			new Box(request).new DropList().printList(request);
			request.getRequestDispatcher("/index.jsp").forward(request, response);
		}
	}

	/**
	 * 
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getParameter("delete") != null) {
			System.out.println("request for deleting: " + request.getParameter("delete"));
			request.setAttribute("index.jsp", "index.jsp");
			new Box(request).new DropDelete().delete(request.getParameter("delete"));
			new Box(request).new DropList().printList(request);
			request.getRequestDispatcher("/index.jsp").forward(request, response);
		} else if (request.getHeader("x-file-name") != null && request.getHeader("x-file-name").endsWith(".mp3")) {
			new Box(request).new DropUpload(request.getInputStream(), request.getHeader("x-file-name"));
		}
	}

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		System.out.println("Session created");
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		System.out.println("Session destroyed");
		DropSession.dropSession(se.getSession());
	}
}
