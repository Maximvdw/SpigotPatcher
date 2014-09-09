package org.spigotmc.patcher;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.jbeat.Patcher;

public class Main {

	public static void main(String[] args) throws Exception {
		boolean forcePatch = false;

		if (args.length < 3) {
			System.out.println("Welcome to the Spigot patch applicator.");
			System.out
					.println("In order to use this tool you will need to specify three command line arguments as follows:");
			System.out
					.println("\tjava -jar SpigotPatcher.jar original.jar patch.bps output.jar");
			System.out
					.println("This will apply the specified patch to the original jar and save it to the output jar");
			System.out
					.println("Please ensure that you save your original jar for later use.");
			System.out
					.println("If you have any queries, please direct them to http://www.spigotmc.org/");
			return;
		}

		File originalFile = new File(args[0]);
		File patchFile = null;
		if (args[1].equals("download")) {
			// Download the latest patch file
			patchFile = downloadPatch();
		} else {
			patchFile = new File(args[1]);
		}
		if (patchFile == null) {
			return;
		}

		File outputFile = new File(args[2]);
		if (args.length == 4) {
			if (args[3].equals("force")) {
				forcePatch = true; // Force the patch (overwrite)
			}
		}

		if (!originalFile.canRead()) {
			System.err.println("Specified original file " + originalFile
					+ " does not exist or cannot be read!");
			return;
		}
		if (!patchFile.canRead()) {
			System.err.println("Specified patch file " + patchFile
					+ " does not exist or cannot be read!!");
			return;
		}
		if (outputFile.exists()) {
			if (!forcePatch) {
				System.err
						.println("Specified output file "
								+ outputFile
								+ " exists, please remove it before running this program!");
				return;
			} else {
				outputFile.delete();
			}
		}
		if (!outputFile.createNewFile()) {
			System.out
					.println("Could not create specified output file "
							+ outputFile
							+ " please ensure that it is in a valid directory which can be written to.");
			return;
		}

		System.out.println("***** Starting patching process, please wait.");
		System.out.println("\tInput md5 Checksum: "
				+ Files.hash(originalFile, Hashing.md5()));
		System.out.println("\tPatch md5 Checksum: "
				+ Files.hash(patchFile, Hashing.md5()));

		try {
			new Patcher(patchFile, originalFile, outputFile).patch();
		} catch (Exception ex) {
			System.err.println("***** Exception occured whilst patching file!");
			ex.printStackTrace();
			outputFile.delete();
			return;
		}

		System.out
				.println("***** Your file has been patched and verified! We hope you enjoy using Spigot!");
		System.out.println("\tOutput md5 Checksum: "
				+ Files.hash(outputFile, Hashing.md5()));
	}

	public static File downloadPatch() {
		List<String> allPatches = new ArrayList<String>();
		try {
			System.out.println("\tChecking spigot for the latest patch ...");
			String url = "http://www.spigotmc.org/spigot-updates/";
			// Fetch the index
			URL address = new URL(url);
			HttpURLConnection con = (HttpURLConnection) address
					.openConnection();
			con.setRequestMethod("GET");

			// Cloudfare doesn't like empty user agents
			con.setRequestProperty("User-Agent", "Mozilla/5.0");

			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			StringBuffer response = new StringBuffer();
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

			String sourceStr = response.toString();
			Matcher m = Pattern.compile("spigot([-+]\\d+)([a-z])\\.bps")
					.matcher(sourceStr);
			while (m.find()) {
				allPatches.add(m.group()); // Patches will be added twice (but
											// we just need the last one)
				// URL + Text
			}
		} catch (Exception ex) {
			// Error
			System.out
					.println("\tSomething went wrong checking for the latest patch!");
			System.out
					.println("\tPlease download it manually from: http://spigotmc.org/spigot-updates/");
		}
		if (allPatches.size() != 0)
			try {
				System.out.println("\tDownloading latest file ["
						+ allPatches.get(allPatches.size() - 1) + "] ...");
				URL address = new URL("http://www.spigotmc.org/spigot-updates/"
						+ allPatches.get(allPatches.size() - 1));
				HttpURLConnection con = (HttpURLConnection) address
						.openConnection();
				con.setRequestMethod("GET");

				// Cloudfare doesn't like empty user agents
				con.setRequestProperty("User-Agent", "Mozilla/5.0");

				File file = new File(allPatches.get(allPatches.size() - 1));
				BufferedInputStream bis = new BufferedInputStream(
						con.getInputStream());
				BufferedOutputStream bos = new BufferedOutputStream(
						new FileOutputStream(file.getName()));
				int i = 0;
				while ((i = bis.read()) != -1) {
					bos.write(i);
				}
				bos.flush();
				bis.close();
				bos.close();
				return file;
			} catch (Exception ex) {
				// Error
				System.out
						.println("\tSomething went wrong downloading the latest patch!");
				System.out
						.println("\tPlease download it manually from: http://spigotmc.org/spigot-updates/");
			}
		return null;
	}
}
