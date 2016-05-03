package steganography;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Main {
	
	private static final String ARGS_ERROR = "Program usage: \n"
											+ "1) Find hidden message: \"AVISteganography.jar R <input_file>\"\n"
											+ "2) Hide message: \"AVISteganography.jar W <input_file> <message> <output_file>\"\n";
	private static final String READ_MODE = "R";
	private static final String WRITE_MODE = "W";
	
	/**
	 * Value that specifies, what the program should do 
	 * (hide message in the file or just find and print the message...).
	 */
	private static String mode;
	
	/**
	 * Patch to the input file.
	 */
	private static String inputFile;
	
	/**
	 * Message, which user wishes to hide.
	 */
	private static String message;
	
	/**
	 * Path to the file, which will be created after the message is hidden in its data.
	 */
	private static String outputFile;
	
	/**
	 * Main method of the program, it calls {@code loadArgs} method to check input arguments.
	 * Then it decides, depending on {@code mode} variable, what action will the program do.
	 * In case of reading the message it calls {@code findMessage} method and prints its result.
	 * In case of writing the message into file it calls {@code hideMessage} method and its result
	 * (byte array of the file with hidden message) passes to {@code writeFile} method.
	 * 
	 * @param args	input parameters of the program
	 */
	public static void main(String[] args) {
		loadArgs(args);
		
		byte[] fileContent = readFile(inputFile);
		
		switch(mode) {
		case READ_MODE:
			System.out.println("Hidden message: "+findMessage(fileContent));
			break;
		case WRITE_MODE:
			fileContent = hideMessage(message, fileContent);
			writeFile(fileContent, outputFile);
			System.out.println("Message '"+message+"' has been hidden into '"+outputFile+"' file.");
		}
	}
	
	/**
	 * Method that checks if input arguments are correctly set. It first checks if {@code args} array
	 * has at least two members and continues with saving the first member into {@code mode} variable
	 * (it also checks for known values). Then it saves path to input file from the second member.
	 * If program is used to hide the message into the file, it continues with checking the length of 4
	 * for {@code args} array and saves the message and path to output file from third and fourth member.
	 * 
	 * @param args	input parameters of the program
	 */
	private static void loadArgs(String[] args) {
		boolean ok = true;
		
		if(args.length >= 2) {
			if(args[0].toUpperCase().equals(READ_MODE)  || args[0].toUpperCase().equals(WRITE_MODE)) {
				mode = args[0].toUpperCase();
			}
			else {
				ok = false;
				System.out.println("Unknown program mode.");
			}
			
			inputFile = args[1];
			
			if(mode.equals(WRITE_MODE)) {
				if(args.length == 4) {
					message = args[2];
					outputFile = args[3];
				}
				else {
					ok = false;
					System.out.println("You have to pass all 4 parameters for writing message into file.");
				}
			}
		}
		else {
			ok = false;
			System.out.println("You have to pass at least 2 parameters.");
		}
		
		if(!ok) {
			System.out.println(ARGS_ERROR);
			System.exit(1);
		}
	}
	
	/**
	 * Method, which reads the input file and saves its bytes into a byte array 
	 * that is returned from the method.
	 * 
	 * @param fileName	path to the file
	 * @return	the byte array with bytes from the file
	 */
	private static byte[] readFile(String fileName) {
		File file = new File(fileName);
		byte[] fileContent = new byte[(int)file.length()];

		try{
			int totalBytesRead = 0;
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
			
			while(totalBytesRead < fileContent.length) {
				int bytesRemaining = fileContent.length - totalBytesRead;
				int bytesRead = input.read(fileContent, totalBytesRead, bytesRemaining);
				if(bytesRead > 0) {
					totalBytesRead = totalBytesRead + bytesRead;
				}
			}
			
			input.close();
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return fileContent;
	}
	
	/**
	 * Method, which writes passed byte array into a file specified by passed path.
	 * 
	 * @param fileContent	byte array that will be saved into the file
	 * @param fileName	path of the output file
	 */
	private static void writeFile(byte[] fileContent, String fileName) {
		File file = new File(fileName);
		try{
			BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
			output.write(fileContent);
			output.close();
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Method, which goal is to hide the message into the byte array - one bit of the message per one byte from the array.
	 * It first calls {@code getVideoStreamPos} method that tries to find the start of the video stream from the file ('movi' list)
	 * and return the number of relevant byte. The position is then moved by 4 bytes forward, where should be first RIFF chunk
	 * with the stream data. Method then loops through individual chunks until there are no more chunks or the message was completely hidden
	 * in the data. The resulting byte array is then returned.
	 *   
	 * @param message	message to be hidden in the data
	 * @param fileContent	byte array from the input file
	 * @return	modified byte array with hidden message
	 */
	private static byte[] hideMessage(String message, byte[] fileContent) {
		byte[] resultContent = Arrays.copyOf(fileContent, fileContent.length);
		int currentPos = 0;
		
		try {
			currentPos = getVideoStreamPos(fileContent);
			System.out.println("Video stream (movi list) found at "+currentPos+". byte.");
			currentPos += 4; //moving to the first chunk
			
			String messageToHide = message;
			int chunkSize;
			while((chunkSize = getNextChunkSize(fileContent, currentPos)) > 0 && messageToHide != null) {
				currentPos += 8; //moving to chunk's data
				messageToHide = saveMessageIntoChunk(messageToHide, resultContent, currentPos, chunkSize);
				currentPos += chunkSize;
			}
		}
		catch(ArrayIndexOutOfBoundsException e) {
			System.out.println("Video stream (movi list) has not been found. Make sure the input file is an actual AVI.");
		}
		
		return resultContent;
	}
	
	private static String findMessage(byte[] fileContent) {
		String message = "";
		int currentPos = 0;
		
		try{
			currentPos = getVideoStreamPos(fileContent);
			System.out.println("Video stream (movi list) found at "+currentPos+". byte.");
			currentPos += 4; //moving to the first chunk
			
			int chunkSize;
			while((chunkSize = getNextChunkSize(fileContent, currentPos)) > 0) {
				currentPos += 8; //moving to chunk's data
				message += getMessageFromChunk(message, fileContent, currentPos, chunkSize);
				currentPos += chunkSize;
			}
			
		}
		catch(ArrayIndexOutOfBoundsException e) {
			System.out.println("Video stream (movi list) has not been found. Make sure the input file is an actual AVI.");
		}
		
		return message;
	}
	
	/**
	 * Method, which cycles through the bytes of the passed array in effort to find 'movi' sequence,
	 * which means beginning of the video stream from AVI file. Once the sequence is found, the index
	 * of the first byte of the sequence is returned.  
	 * @param content	byte array with file content
	 * @return	position of the first byte of the 'movi' sequence.
	 * @throws ArrayIndexOutOfBoundsException	in case that 'movi' sequence won't be found,
	 * 											the while loop will eventually increase the index 
	 * 											out of bounds of the byte array, which also means
	 * 											the input file is probably not AVI
	 */
	private static int getVideoStreamPos(byte[] content) throws ArrayIndexOutOfBoundsException {
		int bytePos = -1;
		String videoMoviListID = "movi";
		String valueFound = "";
		
		while(!valueFound.equals(videoMoviListID)) {
			bytePos++;
			valueFound = "";
			for(int i = bytePos; i < bytePos+4; i++) {
				valueFound += (char)content[i];
			}
		}

		return bytePos;
	}
	
	private static int getNextChunkSize(byte[] content, int chunkPos) {
		int chunkSize = 0;
		
		if((char)content[chunkPos+2] == 'd' && ((char)content[chunkPos+3] == 'b' || (char)content[chunkPos+3] == 'c')) {
			int currPos = chunkPos + 4;
			byte[] sizeBytes = new byte[4];
			
			for(int i = sizeBytes.length - 1; i >= 0; i--) {
				sizeBytes[i] = content[currPos];
				currPos++;
			}
			
			ByteBuffer sizeBytesWrapper = ByteBuffer.wrap(sizeBytes);
			chunkSize = sizeBytesWrapper.getInt();
		}
		
		
		return chunkSize;
	}
	
	private static String saveMessageIntoChunk(String message, byte[] content, int pos, int chunkSize) {
		String remainingMessage = null;
		int messageIndex = 0;
		int charIndex = 0;
		int currPos = pos;
		int overhead = chunkSize % 8;
		
		for(int i = 0; i < (chunkSize - overhead); i++) {
			if(messageIndex < message.length()) {
				char messageChar = message.charAt(messageIndex);
				int bit_ch = (messageChar >> charIndex) & 1;
				content[currPos] = (byte) ((content[currPos] & (byte)0xFE) | (byte)bit_ch);
				currPos++;
				
				if(charIndex > 7) {
					charIndex = 0;
					messageIndex++;
				}
				else {
					charIndex++;
				}
			}
			else {
				break;
			}
		}
		
		if(messageIndex < message.length()) {
			remainingMessage = message.substring(messageIndex);
		}
		
		return remainingMessage;
	}
	
	private static String getMessageFromChunk(String message, byte[] content, int pos, int chunkSize) {
		String resultMessage = message;
		int currPos = pos;
		int overhead = chunkSize % 8;
		int charIndex = 0;
		char messageChar = 0;
		
		for(int i = 0; i < (chunkSize - overhead); i++) {
			int bit_byte = (content[currPos] & (byte)0x01) << charIndex;
			messageChar = (char) (messageChar |  (char)bit_byte);
			
			currPos++;
			
			if(charIndex > 7) {
				resultMessage += messageChar;
				charIndex = 0;
			}
			else {
				charIndex++;
			}
		}
		
		System.out.println(resultMessage);
		return resultMessage;
	}
}
