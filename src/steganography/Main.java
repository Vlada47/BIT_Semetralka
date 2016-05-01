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
	
	
	private static String mode;
	private static String inputFile;
	private static String message;
	private static String outputFile;
	
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
		}
	}
	
	private static void loadArgs(String[] args) {
		boolean ok = true;
		
		if(args.length >= 2) {
			if(args[0].toUpperCase() == READ_MODE || args[0].toUpperCase() == WRITE_MODE) {
				mode = args[0].toUpperCase();
			}
			else {
				ok = false;
			}
			
			inputFile = args[1];
			
			if(mode == WRITE_MODE) {
				if(args.length == 4) {
					message = args[2];
					outputFile = args[3];
				}
				else {
					ok = false;
				}
			}
		}
		else {
			ok = false;
		}
		
		if(!ok) {
			System.out.println(ARGS_ERROR);
		}
	}
	
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
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		
		return fileContent;
	}
	
	private static void writeFile(byte[] fileContent, String fileName) {
		File file = new File(fileName);
		try{
			BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
			output.write(fileContent);
			output.close();
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
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
		
		for(int i = 0; i < chunkSize; i++) {
			if(messageIndex < message.length()) {
				char messageChar = message.charAt(messageIndex);
				//change the bit in the content if need be and move to another character when all bits have been used
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
		int currentPos = pos;
		
		for(int i = 0; i < chunkSize; i++) {
			//add individual bits from chunk's bytes and when the whole character is composed add it to the message
			currentPos++;
		}
		
		return message;
	}
}
