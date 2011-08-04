package georg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;
import com.dropbox.client.DropboxAPI.Entry;
import com.dropbox.client.DropboxAPI.FileDownload;

public class Box {

	private final DropboxAPI api;
	private final HttpServletRequest request;

	public Box(HttpServletRequest request) {
		this.request = request;
		this.api = DropSession.getDropSession(this.request);
	}

	class DropUpload extends Thread {
		private final File file;

		public DropUpload(InputStream is, String fileName) {

			this.file = getFileFromStream(is, getRandomFileName() + "");
			start();
		}

		private File getFileFromStream(InputStream is, String filename) {
			File f = new File(Fritz.tmp_path + "/" + filename);
			try {
				FileOutputStream fos = new FileOutputStream(f);
				byte buf[] = new byte[1024];
				int len;
				while ((len = is.read(buf)) > 0)
					fos.write(buf, 0, len);
				fos.close();
				is.close();
			} catch (Exception e) {
			}
			return f;
		}

		@Override
		public void run() {
			System.out.println("start uploading");
			try {
				api.putFile("dropbox", Fritz.drop_path, file);
				file.delete();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("finished uploading");

			ArrayList<Entry> files = new DropList().getList();
			createPodCast_xml(files);
			createStreamList_m3u(files, "list");
		}

		private int getRandomFileName() {
			return (int) (Math.random() * 999999999);
		}
	}

	private String geturlFromFileName(String fileName) {
		return Fritz.www_path + "/Fritz?id=" + fileName;
	}

	public void writeFile(String fileName, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(Fritz.root_path + "/" + fileName);
			Writer out = new OutputStreamWriter(fos, "UTF8");
			out.write(content);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getMp3Duration(File f) {
		long duration = 0L;
		try {
			duration = new Long(new MP3File(f).getID3v2Tag().getFrame("TLEN").getBody().getObject("Text") + "") / 1000L;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TagException e) {
			e.printStackTrace();
		}
		return "" + duration;
	}

	public void createStreamList_m3u(ArrayList<Entry> files, String fileName) {
		Collections.shuffle(files);
		String m3u = "";
		m3u += "#EXTM3U\n";
		//		m3u += Fritz.www_path + "?id=88\n";
		for (Entry file : files) {
			//			m3u += "#EXTINF:221,\n";
			m3u += geturlFromFileName(file.fileName()) + "\n";
		}
		writeFile(fileName + ".m3u", m3u);
	}

	public void createPodCast_xml(ArrayList<Entry> files) {
		String xml = "";
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		xml += "<?xml-stylesheet href=\"/resources/xsl/podcast.xsl\" type=\"text/xsl\"?>\n";
		xml += "<rss version=\"2.0\" ";
		xml += "xmlns:content=\"http://purl.org/rss/1.0/modules/content/\" ";
		xml += "xmlns:wfw=\"http://wellformedweb.org/CommentAPI/\" ";
		xml += "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" ";
		xml += "xmlns:itunes=\"http://www.itunes.com/dtds/podcast-1.0.dtd\" >\n";
		xml += "\t<channel>\n";
		xml += "\t\t<title>Music</title>\n";
		xml += "\t\t<language>de-de</language>\n";

		for (Entry file : files) {
			//			try {
			//				ID3v1 tag = new ID3v1(new RandomAccessFile(new File(""), "r"));
			//			} catch (Exception e) {
			//				e.printStackTrace();
			//			}
			//			getfromTagMp3($dir."/".$file);$title=$title["song"]." - ".$title["artist"];
			String title = "";
			xml += "\t\t<item>\n";
			xml += "\t\t\t<title>" + title + "</title>\n";
			xml += "\t\t\t<enclosure url=\"" + geturlFromFileName(file.fileName()) + "\" type=\"audio/mpeg\" />\n";
			xml += "\t\t</item>\n";
		}
		xml += "\t</channel>\n";
		xml += "</rss>\n";
		writeFile("list.xml", xml);
	}

	class DropDownload {
		private final String fileName;

		public DropDownload(String fileName) {
			this.fileName = fileName;
		}

		public void download(HttpServletResponse response) {
			System.out.println("start downloading");
			try {
				FileDownload fd = api.getFileStream("dropbox", Fritz.drop_path + "/" + fileName, fileName);
				response.setContentType("audio/mpeg");
				response.setContentLength((int) fd.length);
				// Set to expire far in the past.
				//				response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");

				// Set standard HTTP/1.1 no-cache headers.
				//				response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

				// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
				//				response.addHeader("Cache-Control", "post-check=0, pre-check=0");

				// Set standard HTTP/1.0 no-cache header.
				//				response.setHeader("Pragma", "no-cache");

				response.setHeader("Content-Disposition", "inline;filename=" + fileName);
				ServletOutputStream out = response.getOutputStream();

				//				byte[] outputByte = new byte[1024];
				//				while (fd.is.read(outputByte, 0, 1024) != -1) {
				//					out.write(outputByte, 0, 1024);
				//				}
				int i;
				while ((i = fd.is.read()) != -1) {
					out.write(i);
				}
				fd.is.close();
				out.flush();
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("finished downloading");

		}
	}

	class DropStream {

		public void stream(HttpServletResponse response, String sessionID) {
			System.out.println("start streaming");
			try {

				response.setContentType("audio/mpeg");

				// Set to expire far in the past.
				//response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");

				// Set standard HTTP/1.1 no-cache headers.
				response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

				// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
				response.addHeader("Cache-Control", "post-check=0, pre-check=0");

				// Set standard HTTP/1.0 no-cache header.
				response.setHeader("Pragma", "no-cache");

				response.setHeader("Content-Disposition", "inline; filename=stream.mp3");
				ServletOutputStream out = response.getOutputStream();
				int i;
				ArrayList<Entry> files = new DropList().getList();
				Collections.shuffle(files);
				for (Entry file : files) {
					FileDownload fd = api.getFileStream("dropbox", Fritz.drop_path + "/" + file.fileName(), "stream.mp3");
					byte[] b = new byte[1024];

					for (i = 0; i < 128; i++)
						fd.is.read();
					while (fd.is.read(b, 0, 1024) != -1) {
						try {
							out.write(b, 0, 1024);
						} catch (Exception e) {
						}

					}
					System.out.println("nextSong....");
				}
				out.flush();
				out.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("finished streaming");

		}
		//creates if not exist yet a sessionial m3u file and send it
		//		public void stream(HttpServletResponse response, String sessionID) {
		//			System.out.println("start streaming");
		//			try {
		//				File file = new File(Fritz.root_path + "/list_" + sessionID + ".m3u");
		//				if (!file.exists()) {
		//					ArrayList<Entry> files = new DropList().getList();
		//					createStreamList_m3u(files, "list_" + sessionID);
		//				}
		//
		//				FileInputStream fis = new FileInputStream(file);
		//
		//				response.setContentType("audio/x-mpegurl");
		//				response.setContentLength((int) file.length());
		//
		//				// Set to expire far in the past.
		//				//response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");
		//
		//				// Set standard HTTP/1.1 no-cache headers.
		//				//response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
		//
		//				// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
		//				//response.addHeader("Cache-Control", "post-check=0, pre-check=0");
		//
		//				// Set standard HTTP/1.0 no-cache header.
		//				///response.setHeader("Pragma", "no-cache");
		//
		//				response.setHeader("Content-Disposition", "attachment; filename=list_" + sessionID + ".m3u");
		//				ServletOutputStream out = response.getOutputStream();
		//				//				byte[] outputByte = new byte[1024];
		//				int i;
		//				while ((i = fis.read()) != -1) {
		//					out.write(i);
		//				}
		//				fis.close();
		//				out.flush();
		//				out.close();
		//
		//			} catch (Exception e) {
		//				e.printStackTrace();
		//			}
		//			System.out.println("finished streaming");
		//
		//		}
	}

	class DropList {

		public DropList() {
		}

		public void printList(HttpServletRequest request) {
			System.out.println("start listing");
			try {
				Entry entry = api.metadata("dropbox", Fritz.drop_path, 1000, "", true);
				request.setAttribute("filelist", entry.contents);

			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("finished listing");

		}

		public ArrayList<Entry> getList() {
			Entry entry = api.metadata("dropbox", Fritz.drop_path, 1000, "", true);
			return entry.contents;
		}
	}

	class DropDelete {

		public DropDelete() {
		}

		public void delete(String fileName) {
			System.out.println("start deleting");
			try {
				api.delete("dropbox", Fritz.drop_path + "/" + fileName);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("finished deleting");

		}
	}

}

class DropSession {

	private static HashMap<String, String> secrets = new HashMap<String, String>();
	private static HashMap<String, DropboxAPI> apis = new HashMap<String, DropboxAPI>();

	private static Config config;

	public static DropboxAPI getDropSession(HttpServletRequest request) {
		//		HttpSession s = request.getSession(false);
		//		s.setMaxInactiveInterval(KonfigFiles.getInt(KonfigFiles.FRITZ_SESSION_TIME));
		//		Object atk = s.getAttribute("accessTokenKey");
		DropboxAPI api = null;
		//		if (atk != null && secrets.get(atk) != null) {
		//			String ats = secrets.get(atk);
		//			api = apis.get(ats);
		//			if (!api.isAuthenticated()) {
		//				api = authenticate(api, getConfig(api), atk.toString(), ats);
		//				apis.put(ats, api);
		//				if (!api.isAuthenticated()) {
		//					secrets.remove(ats);
		//					apis.remove(api);
		//
		//					Config conf = authenticate_full(api, getConfig(api), Fritz.drop_user, Fritz.drop_pw);
		//					api = authenticate(api, conf, conf.accessTokenKey, conf.accessTokenSecret);
		//					apis.put(conf.accessTokenSecret, api);
		//					s.setAttribute("accessTokenKey", conf.accessTokenKey);
		//					secrets.put(conf.accessTokenKey, conf.accessTokenSecret);
		//				}
		//			}
		//		} else {
		//			api = new DropboxAPI();
		//			Config conf = authenticate_full(api, getConfig(api), Fritz.drop_user, Fritz.drop_pw);
		//			api = authenticate(api, conf, conf.accessTokenKey, conf.accessTokenSecret);
		//			apis.put(conf.accessTokenSecret, api);
		//			s.setAttribute("accessTokenKey", conf.accessTokenKey);
		//			secrets.put(conf.accessTokenKey, conf.accessTokenSecret);
		//		}
		api = apis.get("0");
		if (api != null) {
			System.out.println("api exists");
			if (!api.isAuthenticated()) {
				System.out.println("api needed to  authenticate");
				Config conf = authenticate_full(api, getConfig(api), Fritz.drop_user, Fritz.drop_pw);
				api = authenticate(api, conf, conf.accessTokenKey, conf.accessTokenSecret);
			}
		} else {
			System.out.println("api not exists");
			api = new DropboxAPI();
			Config conf = authenticate_full(api, getConfig(api), Fritz.drop_user, Fritz.drop_pw);
			api = authenticate(api, conf, conf.accessTokenKey, conf.accessTokenSecret);
			apis.put("0", api);
		}

		return api;
	}

	public static void dropSession(HttpSession s) {
		Object atk = s.getAttribute("accessTokenKey");
		DropboxAPI api = null;
		if (atk != null && secrets.get(atk) != null) {
			String ats = secrets.get(atk);
			api = apis.get(ats);
			if (api != null && api.isAuthenticated()) {
				api.deauthenticate();
				System.out.println("Session deleted!");
			}
			s.removeAttribute("accessTokenKey");
			apis.remove(ats);
			secrets.remove(atk);
		}
		File file = new File(Fritz.root_path + "/list_" + s.getId() + ".m3u");
		if (file.exists()) {
			file.delete();
		}
		s.removeAttribute("accessTokenKey");
		s.invalidate();
	}

	private static Config getConfig(DropboxAPI api) {
		if (config == null) {
			System.setProperty("java.net.useSystemProxies", "true");
			Map<String, Object> configuration = new HashMap<String, Object>();
			configuration.put("consumer_key", "acwhw47d1o88jkl");
			configuration.put("consumer_secret", "zfeorz9kdzgfkjd");
			configuration.put("request_token_url", "http://api.dropbox.com/0/oauth/request_token");
			configuration.put("access_token_url", "http://api.dropbox.com/0/oauth/access_token");
			configuration.put("authorization_url", "http://api.dropbox.com/0/oauth/authorize");
			configuration.put("port", 80);
			configuration.put("trusted_access_token_url", "http://api.getdropbox.com/0/token");
			configuration.put("server", "api.dropbox.com");
			configuration.put("content_server", "api-content.dropbox.com");
			config = api.new Config(configuration);
		}
		return config;
	}

	private static Config authenticate_full(DropboxAPI api, Config conf, String username, String password) {
		conf = api.authenticate(conf, username, password);
		api.authenticateToken(conf.accessTokenKey, conf.accessTokenSecret, conf);
		return config;
	}

	private static DropboxAPI authenticate(DropboxAPI api, Config conf, String tokenKey, String tokenSecret) {
		api.authenticateToken(tokenKey, tokenSecret, conf);
		return api;
	}
}
