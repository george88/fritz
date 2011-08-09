package georg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import org.apache.james.mime4j.codec.ByteQueue;
import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;

import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;
import com.dropbox.client.DropboxAPI.Entry;
import com.dropbox.client.DropboxAPI.FileDownload;

public class Box {

	private DropboxAPI api;
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	ServletOutputStream out;

	public Box(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
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

			ArrayList<Entry> files = new DropList().getList(null);
			createPodCast_xml(files);
			createStreamList_m3u(files, "list");
		}

		private int getRandomFileName() {
			return (int) (Math.random() * 999999999);
		}
	}

	private FileInputStream getStreamFromFile(File file) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return fis;
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
			try {
				out = response.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void download() {
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
		String sessionID;
		String stream;
		Queue<byte[]> bq = new LinkedList<byte[]>();
		ArrayList<byte[]> pauseBytes = new ArrayList<byte[]>();
		boolean writeWait = false;
		long pufferSize = 10000;

		public DropStream() {
			this.sessionID = request.getSession(false).getId();
			this.stream = request.getParameter("stream");
			try {
				out = response.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void startStream() {
			WriteStream ws = new WriteStream();
			ws.start();
			try {
				byte[] pb = new byte[128];
				FileInputStream fis = new FileInputStream(Fritz.root_path + "/ansagen/radio_georg.mp3");
				while (fis.read(pb, 0, 128) >= pb.length)
					pauseBytes.add(pb);

			} catch (Exception e2) {
				e2.printStackTrace();
			}

			try {
				Thread.sleep(1500);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				while (true) {
					//					if (writeWait && bq.size() < pufferSize - 100) {
					//						writeWait = false;
					//						System.out.println("try to wake up");
					//						ws.notify();
					//					}
					//					byte[] b = bq.poll();
					if (bq.size() > 10000) {
						synchronized (bq) {
							out.write(bq.poll(), 0, 128);
						}
					} else {
						//						ansage();
						//						System.out.println("p");
						//						for (int i = 0; i < pauseBytes.size(); i++)
						//							out.write(pauseBytes.get(i));
					}
				}
			} catch (Exception e) {
				try {
					ws.interrupt();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}

		class WriteStream extends Thread implements Runnable {
			@Override
			public void run() {
				while (true) {
					//					if (bq.size() > pufferSize + 100) {
					//						System.out.println("try to sleep");
					//						writeWait = true;
					//						try {
					//							Thread.currentThread().wait();
					//						} catch (InterruptedException e) {
					//							e.printStackTrace();
					//						}
					//					}
					ArrayList<Entry> files = new DropList().getList(stream);
					Collections.shuffle(files);
					for (Entry file : files) {
						FileDownload fd = null;

						if (api != null && api.isAuthenticated())
							try {
								fd = api.getFileStream("dropbox", Fritz.drop_path + "/" + file.fileName(), "stream.mp3");

							} catch (Exception e) {
								api = DropSession.getDropSession(request);
								fd = api.getFileStream("dropbox", Fritz.drop_path + "/" + file.fileName(), "stream.mp3");
							}
						else {
							api = DropSession.getDropSession(request);
							fd = api.getFileStream("dropbox", Fritz.drop_path + "/" + file.fileName(), "stream.mp3");
						}

						if (fd == null || (fd != null && fd.is == null))
							continue;
						byte[] b = new byte[128];
						try {
							System.out.println("start downloading");
							while ((fd.is.read(b, 0, b.length)) != -1) {
								synchronized (bq) {
									bq.add(b);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		DropStreamThread dst;
		AnsageStreamThread ast;

		public void stream() {
			dst = new DropStreamThread();
			dst.run();
		}

		class DropStreamThread extends Thread {
			@Override
			public void run() {

				System.out.println("id: " + sessionID + ".....start streaming....");
				try {
					while (true) {
						response.setContentType("audio/mpeg");
						// Set to expire far in the past.
						//				response.setHeader("Expires", "Sat, 6 May 1995 12:00:00 GMT");

						// Set standard HTTP/1.1 no-cache headers.
						//response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

						// Set IE extended HTTP/1.1 no-cache headers (use addHeader).
						//response.addHeader("Cache-Control", "post-check=0, pre-check=0");

						// Set standard HTTP/1.0 no-cache header.
						//				response.setHeader("Pragma", "no-cache");
						response.setHeader("Content-Disposition", "inline; filename=stream.mp3");
						//			startStream();
						//			return;
						ArrayList<Entry> files = new DropList().getList(stream);
						Collections.shuffle(files);
						//				response.setContentLength(-1);
						//				response.setHeader("Content-Length", "-1");

						for (Entry file : files) {
							System.out.println("id: " + sessionID + "......start downloading");
							FileDownload fd = null;

							if (api != null && api.isAuthenticated())
								try {
									fd = api.getFileStream("dropbox", Fritz.drop_path + "/" + file.fileName(), "stream.mp3");

								} catch (Exception e) {
									api = DropSession.getDropSession(request);
									fd = api.getFileStream("dropbox", Fritz.drop_path + "/" + file.fileName(), "stream.mp3");
								}
							else {
								api = DropSession.getDropSession(request);
								fd = api.getFileStream("dropbox", Fritz.drop_path + "/" + file.fileName(), "stream.mp3");
							}
							if (fd == null) {
								System.out.println("fd == null");
								continue;
							}

							if (fd.is == null) {
								System.out.println("fd.is == null");

								continue;
							}

							int i = 0;
							while (i < 16) {
								fd.is.skip(128);
								i++;
							}
							int buffering = 8192;
							byte[] b = new byte[buffering];
							long length = fd.length - 2048;
							while (fd.is.read(b, 0, buffering) != -1) {
								synchronized (b) {

									out.write(b, 0, buffering);

									length -= buffering;
									if (length < 2048) {
										System.out.println("length < 2048");
										break;
									}
								}
							}

							System.out.println("id:" + sessionID + " ....nextSong....");
						}
					}
					//					out.flush();
					//					out.close();

				} catch (Exception e) {
					e.printStackTrace();

				}
				System.out.println("id: " + sessionID + ".....finished streaming....");
			}
		}

		AudioFormat audioFormat;
		DataLine.Info dataLineInfo;
		TargetDataLine targetDataLine;

		class AnsageStreamThread extends Thread {
			@Override
			public void run() {
				ansage();
			}
		}

		private void ansage() {
			System.out.println("start ansage...");
			try {

				FileInputStream fis = new FileInputStream(Fritz.root_path + "/ansagen/radio_georg.mp3");
				byte[] b = new byte[128];
				while ((fis.read(b, 0, b.length)) != -1) {
					//synchronized (out) {
					out.write(b, 0, b.length);
					//}
				}
			} catch (Exception e) {
				System.out.print(sessionID);
				e.printStackTrace();
			}
			System.out.println("ende ansage...");
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

		public void printList() {
			System.out.println("start listing");
			try {
				Entry entry = api.metadata("dropbox", Fritz.drop_path, 1000, "", true);
				request.setAttribute("filelist", entry.contents);

			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("finished listing");

		}

		public ArrayList<Entry> getList(String dir) {
			Entry entry = api.metadata("dropbox", Fritz.drop_path + (dir != null && !dir.equals("ON") ? "/" + dir : ""), 1000, "", true);
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
		HttpSession s = request.getSession();
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