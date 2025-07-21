package com.archer.net.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.archer.net.http.HttpException;
import com.archer.net.http.HttpRequest;
import com.archer.net.http.HttpStatus;
import com.archer.net.http.multipart.Multipart;
import com.archer.net.http.multipart.MultipartParser;
import com.archer.net.http.multipart.MultipartType;

public class MultipartTest {



	public static final String MULTIPART_HEADER = "multipart/form-data; boundary=";
	
	private static final Random r = new Random();
	
	private static final String PART_BODY = "\r\n\r\n";
	private static final String LINE_LR = "\r\n";
	private static final String HEADER_SEP = "; ";
	private static final String CONTENT_DISPOS = "Content-Disposition: form-data; ";
	private static final String CONTENT_TYPE = "Content-Type: ";
	private static final String EQ = "=\"";
	private static final String EQ_SEP = "\"; ";
	private static final String EQ_END = "\"";
	private static final String NAME = "name";
	private static final String FILENAME = "filename";
	
    public static List<Multipart> parse(String bodyStr, String bd) throws UnsupportedEncodingException {
        String sep = "--"+bd;
        String start = sep + "\r\n", end = sep + "--";
        if(!bodyStr.startsWith(start)) {
        	if(!bodyStr.endsWith(end)) {
        		end += "\r\n";
            	if(!bodyStr.endsWith(end)) {
            		throw new HttpException(HttpStatus.BAD_REQUEST.getCode(), 
            				"invalid mulipart formdata content");
            	}
        	}
        }
        bodyStr = bodyStr.substring(start.length(), bodyStr.length() - end.length());
        return parse(bodyStr, sep + "\r\n", "utf-8");
    }
    
    public static String generateBoundary() {
    	String s = "";
    	for(int i = 0; i < 24; i++) {
    		s += r.nextInt(10);
    	}
    	return "--------------------------" + s;
    }
    
    public static String generateMultipartBody(List<Multipart> parts, String boundary) {
    	String sep = "--" + boundary;
    	StringBuilder sb = new StringBuilder();
    	parts.sort((o1, o2) -> {
    		if(o1.getType() == MultipartType.FILE && o1.getType() == MultipartType.TEXT) {
    			return 1;
    		}
    		return -1;
    	});
    	for(Multipart p: parts) {
    		sb.append(sep).append(LINE_LR);
    		if(p.getType() == MultipartType.TEXT) {
        		sb.append(CONTENT_DISPOS).append(NAME).append(EQ).append(p.getName()).append(EQ_END)
        		.append(LINE_LR).append(LINE_LR)
        		.append(new String(p.getContent(), StandardCharsets.UTF_8))
        		.append(LINE_LR);
    		} else {
        		sb.append(CONTENT_DISPOS).append(NAME).append(EQ).append(p.getName()).append(EQ_SEP).append(FILENAME).append(EQ).append(p.getFileName()).append(EQ_END)
        		.append(LINE_LR)
        		.append(CONTENT_TYPE).append(p.getContentType())
        		.append(LINE_LR).append(LINE_LR)
        		.append(new String(p.getContent(), StandardCharsets.UTF_8))
        		.append(LINE_LR);
    		}
    	}
		sb.append(sep).append("--");
    	return sb.toString();
    }
    
    private static List<Multipart> parse(String body, String sep, String encoding) throws UnsupportedEncodingException {
    	List<Multipart> parts = new LinkedList<>();
    	String[] partStrs = body.split(sep);
    	for(String s: partStrs) {
    		Multipart part = new Multipart();
    		int off = s.indexOf(PART_BODY);
    		if(off < 32) { // "Content-Disposition: ; name=;"  etc
    			throw new HttpException(HttpStatus.BAD_REQUEST);
    		}
    		String partHead = s.substring(0, off);
    		String content = s.substring(off + PART_BODY.length());
    		parseHeader(partHead, part);
    		if(content.endsWith(LINE_LR)) {
    			content = content.substring(0, content.length() - LINE_LR.length());
    		}
    		part.setContent(content.getBytes(encoding));
    		parts.add(part);
    	}
    	return parts;
    }
    
    
    private static void parseHeader(String headerStr, Multipart part) {
    	part.setType(MultipartType.TEXT);
    	String[] headers = headerStr.split(LINE_LR);
    	for(String header: headers) {
    		if(header.startsWith(CONTENT_DISPOS)) {
    			header = header.substring(CONTENT_DISPOS.length());
    			for(String item: header.split(HEADER_SEP)) {
    				int eq = item.indexOf(EQ);
    				if(eq < 0) {
    					continue;
    				}
    				String k = item.substring(0, eq);
    				String v = item.substring(eq + 2, item.length() - 1);
    				if(NAME.equals(k)) {
    					part.setName(v);
    				} else if(FILENAME.equals(k)) {
    					part.setFileName(v);
    					part.setType(MultipartType.FILE);
    				}
    			}
    		} else if(header.startsWith(CONTENT_TYPE)) {
    			part.setContentType(header.substring(CONTENT_TYPE.length()).trim());
    		}
    	}
    }
    
    public static void main(String args[]) {
    	try {
    		

    		List<Multipart> parts = new ArrayList<>();
    		parts.add(new Multipart("Node-Id", "alice"));
    		try {
    			parts.add(new Multipart("file", "data1029.csv", Files.readAllBytes(Paths.get("D:/da.csv")), "application/csv"));
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		String boundary = MultipartParser.generateBoundary();
    		String body = MultipartParser.generateMultipartBody(parts, boundary);
    		
			List<Multipart> pparts = parse(body, boundary);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
