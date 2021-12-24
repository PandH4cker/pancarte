package client.utils;

import java.io.*;

public final class Files {
    public static boolean isSameFiles(File f1, File f2) {
		if (f1.length() != f2.length())
			return false;

		try {
			BufferedInputStream bis1 = new BufferedInputStream(new FileInputStream(f1));
			BufferedInputStream bis2 = new BufferedInputStream(new FileInputStream(f2));

			int ch = 0;
			while ((ch = bis1.read()) != -1)
				if (ch != bis2.read())
					return false;
		} catch(IOException e) {
			e.printStackTrace();
		}
		return true;
	}

    public static void truncateFile(File f) {
		try {new PrintWriter(f).close();}
		catch (IOException e) {e.printStackTrace();}
	}
}