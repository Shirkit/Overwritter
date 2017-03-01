package com.github.shirkit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

public class Overwritter {

	/**
	 * Allows
	 * @param args {@code args[0]} replace or generate. {@code args[1]} must be source folder and {@code args[2]} must be the destination
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args != null && args.length > 0) {
			if (args[0].equalsIgnoreCase("replace")) {
				if (args.length != 3)
					throw new IllegalArgumentException(
							"This mode needs 2 additional args: SOURCE_FOLDER_WITH_OVERWRITE and DESTINATION_FOLDER_TO_BE_OVERWRITTEN");
				ReplaceFiles(new String[] { args[1], args[2] });
				System.exit(0);
			} else if (args[0].equalsIgnoreCase("generate")) {
				if (args.length != 3)
					throw new IllegalArgumentException(
							"This mode needs 2 additional args: SOURCE_FOLDER_WITH_OVERWRITE and DESTINATION_FOLDER_TO_BE_OVERWRITTEN");
				GenerateMD5(new String[] { args[1], args[2] });
				System.exit(0);
			}
		}

		throw new IllegalArgumentException("Invalid mode. Modes available: REPLACE and GENERATE");
	}

	public static void GenerateMD5(String[] args) throws NoSuchAlgorithmException, IOException {

		File dirWithOverwrite = new File(args[0]);
		File dirToOverwrite = new File(args[1]);

		List<File> navigate = inverseNavigate(dirWithOverwrite);

		for (File file : navigate) {
			String replaceAllString = replaceAllString(file.getParent(), dirWithOverwrite.getAbsolutePath(), null);
			if (replaceAllString.startsWith(File.separator))
				replaceAllString = replaceAllString.substring(1);
			
			System.out.println("replaceall: " + replaceAllString);

			File targetFile = new File(dirToOverwrite, replaceAllString);
			if (targetFile.exists()) {
				MessageDigest md = MessageDigest.getInstance("MD5");

				md.update(Files.readAllBytes(Paths.get(targetFile.getAbsolutePath())));
				String targetMD5 = DatatypeConverter.printHexBinary(md.digest()).toUpperCase().trim();

				md.update(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
				String replacingMD5 = DatatypeConverter.printHexBinary(md.digest()).toUpperCase().trim();

				if (targetMD5.equals(replacingMD5))
					System.out.println("Skipping already replaced file: " + targetFile.getAbsolutePath());
				else {
					System.out.println("Calculating needed hash for file: " + targetFile.getAbsolutePath());
					List<CharSequence> cs = new ArrayList<>();
					cs.add(targetMD5);
					File md5 = new File(file.getAbsolutePath() + ".MD5");
					if (md5.exists() && !md5.delete())
						throw new AccessDeniedException("Could not delete file: " + md5.getAbsolutePath());
					Files.write(Paths.get(md5.getAbsolutePath()), cs, StandardOpenOption.CREATE);
				}

			} else
				throw new FileNotFoundException(
						"Target file to be replaced was not found: " + targetFile.getAbsolutePath());
		}
	}

	public static void ReplaceFiles(String[] args) throws NoSuchAlgorithmException, IOException {

		File dirWithOverwrite = new File(args[0]);
		File dirToOverwrite = new File(args[1]);

		List<File> navigate = navigate(dirWithOverwrite);

		for (File md5File : navigate) {
			String replaceAllString = replaceAllString(md5File.getParent(), dirWithOverwrite.getAbsolutePath(), null);
			if (replaceAllString.startsWith(File.separator))
				replaceAllString = replaceAllString.substring(1);

			File targetFile = new File(dirToOverwrite, replaceAllString);
			if (targetFile.exists()) {
				MessageDigest md = MessageDigest.getInstance("MD5");

				md.update(Files.readAllBytes(Paths.get(targetFile.getAbsolutePath())));
				String targetMD5 = DatatypeConverter.printHexBinary(md.digest()).toUpperCase().trim();

				String replacingPath = md5File.getAbsolutePath().substring(0,
						md5File.getAbsolutePath().lastIndexOf(".MD5"));
				md.update(Files.readAllBytes(Paths.get(replacingPath)));
				String replacingMD5 = DatatypeConverter.printHexBinary(md.digest()).toUpperCase().trim();

				String originalMD5 = new String(Files.readAllBytes(Paths.get(md5File.getAbsolutePath()))).trim()
						.toUpperCase();

				if (targetMD5.equals(replacingMD5)) {
					// File already replaced
					System.out.println("Skipping already replaced file: " + targetFile.getAbsolutePath());
				} else if (targetMD5.equals(originalMD5)) {
					// File needs to be replaced
					System.out.println(
							"Replacing file at: " + targetFile.getAbsolutePath() + " - with file - " + replacingPath);
					Files.copy(Paths.get(replacingPath), Paths.get(targetFile.getAbsolutePath()),
							StandardCopyOption.REPLACE_EXISTING);
				} else
					throw new InvalidObjectException(
							"File is on a different version than expected: " + targetFile.getAbsolutePath());
			} else
				throw new FileNotFoundException(
						"Target file to be replaced was not found: " + targetFile.getAbsolutePath());
		}

	}

	public static List<File> navigate(File folder) {
		List<File> _return = new ArrayList<>();
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				_return.addAll(navigate(file));
			} else {
				if (file.getName().endsWith("MD5")) {
					_return.add(file);
				}
			}
		}
		return _return;
	}

	public static List<File> inverseNavigate(File folder) {
		List<File> _return = new ArrayList<>();
		for (File file : folder.listFiles()) {
			if (file.isDirectory()) {
				_return.addAll(inverseNavigate(file));
			} else {
				if (!file.getName().endsWith("MD5")) {
					_return.add(file);
				}
			}
		}
		return _return;
	}

	public static String replaceAllString(String strOrig, String strFind, String strReplace) {
		if (strOrig == null) {
			return null;
		}
		StringBuffer sb = new StringBuffer(strOrig);
		String toReplace = "";

		if (strReplace == null)
			toReplace = "";
		else
			toReplace = strReplace;

		int pos = strOrig.length();

		while (pos > -1) {
			pos = strOrig.lastIndexOf(strFind, pos);
			if (pos > -1)
				sb.replace(pos, pos + strFind.length(), toReplace);
			pos = pos - strFind.length();
		}

		return sb.toString();
	}

}
