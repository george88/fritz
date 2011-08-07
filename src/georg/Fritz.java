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
		System.out.println("urL: " + request.getRequestURL());
		System.out.println("user_agnet: " + request.getHeader("user-agent"));
		if (request.getParameter("id") != null) {
			System.out.println("request: download => id = " + request.getParameter("id"));
			new Box(request).new DropDownload(request.getParameter("id")).download(response);
		} else if (request.getParameter("stream") != null) {
			System.out.println("request: stream");
			new Box(request).new DropStream().stream(response, request.getSession().getId(), request.getParameter("stream"));
		} else if (request.getParameter("reset") != null) {
			System.out.println("request: reset");
			KonfigFiles.reset();
			request.getRequestDispatcher("/index.jsp").forward(request, response);
		} else if (request.getParameter("podcast") != null) {
			System.out.println("request: podcast");
			Box box = new Box(request);
			box.createPodCast_xml(box.new DropList().getList(null));
			request.getRequestDispatcher("/list.xml").forward(request, response);
		} else {
			System.out.println("request: default");
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
		System.out.println("Session created with id = " + se.getSession().getId());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		System.out.println("Session destroyed with id = " + se.getSession().getId());
		DropSession.dropSession(se.getSession());
	}

	public static void download() {

	}
}
