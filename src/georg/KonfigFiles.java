package georg;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Properties;

public class KonfigFiles {

	private static final String fileType = ".properties";
	private static final String konfigPath = "conf";

	public static final String FRITZ_WWW_PATH = "FRITZ_www_path";
	public static final String FRITZ_ROOT_PATH = "FRITZ_root_path";
	public static final String FRITZ_TMP_PATH = "FRITZ_tmp_path";
	public static final String FRITZ_DROP_PATH = "FRITZ_drop_path";
	public static final String FRITZ_DROP_USER = "FRITZ_drop_user";
	public static final String FRITZ_DROP_PW = "FRITZ_drop_pw";
	public static final String FRITZ_SESSION_TIME = "FRITZ_session_time";

	public static HashMap<String, String> props = new HashMap<String, String>();

	public static String getString(String key) {
		String file = key.substring(0, key.indexOf("_"));
		key = key.substring(key.indexOf("_") + 1);
		if (props.containsKey(key))
			return props.get(key);
		try {
			Properties p = new Properties();
			p.load(new KonfigFiles().readFile(file));
			String prop = p.getProperty(key);
			if (prop != null)
				props.put(key, prop);
			return prop;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int getInt(String key) {
		String file = key.substring(0, key.indexOf("_"));
		key = key.substring(key.indexOf("_") + 1);
		if (props.containsKey(key))
			return Integer.parseInt(props.get(key));
		try {
			Properties p = new Properties();
			p.load(new KonfigFiles().readFile(file));
			String prop = p.getProperty(key);
			if (prop != null)
				props.put(key, prop);
			return Integer.parseInt(prop);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public static double getDouble(String key) {
		String file = key.substring(0, key.indexOf("_"));
		key = key.substring(key.indexOf("_") + 1);
		if (props.containsKey(key))
			return Double.parseDouble(props.get(key));
		try {
			Properties p = new Properties();
			p.load(new KonfigFiles().readFile(file));
			String prop = p.getProperty(key);
			if (prop != null)
				props.put(key, prop);
			return Double.parseDouble(prop);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	public InputStreamReader readFile(String konfigFile) throws Exception {
		String path = this.getClass().getResource("../../../").toString();
		path = path.substring(0, path.indexOf("Fritz/"));
		URI uri = new URI(path + konfigPath + "/" + konfigFile + fileType);
		File file = new File(uri);

		return new InputStreamReader(new FileInputStream(file), "UTF8");
	}
}
