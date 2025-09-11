package com.archer.net.http.multipart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.archer.net.Bytes;

public class FormData {

	public static final int CACHE_SIZE = 4 * 1024;
	
	private List<Multipart> parts;
	private String boundary;
	
	private FileInputStream reader;
	private int partsIndex = 0;
	private int fileOff = 0;
	
	public FormData() {
		parts = new ArrayList<>();
		boundary = MultipartParser.generateBoundary();
	}
	
	public void put(String key, String val) {
		parts.add(new Multipart(key, val));
	}

	public void put(String key, String filename, byte[] content) {
		parts.add(new Multipart(key, filename, content));
	}

	public void put(String key, String filename, InputStream input) {
		parts.add(new Multipart(key, filename, input));
	}

	public void put(String key, File file) throws FileNotFoundException {
		parts.add(new Multipart(key, file));
	}

	public List<Multipart> getMultiparts() {
		return parts;
	}

	public String getBoundary() {
		return boundary;
	}
	
	public long calculateFormDataLength() throws IOException {
		long len = 0;
		String sep = "--" + boundary;
    	for(Multipart p: parts) {
			len += String.format("%s\r\n", sep).getBytes(StandardCharsets.UTF_8).length;
			len += String.format("Content-Disposition: form-data; name=\"%s\"", p.getName()).getBytes(StandardCharsets.UTF_8).length;
    		if(p.getType() == MultipartType.FILE) {
                len += String.format("; filename=\"%s\"\r\n", p.getFileName()).getBytes(StandardCharsets.UTF_8).length;
                len += String.format("Content-Type: %s\r\n\r\n", p.getContentType()).getBytes(StandardCharsets.UTF_8).length;
                if(p.getContent() != null) {
                	len += p.getContent().length;
                } else if(p.getFile() != null) {
                	len += p.getFile().length() + 2;
                } else if(p.getInput() != null) {
                	len += p.getInput().available() + 2;
                } else {
                	len += 2;
                }
    		} else {
                len +=  String.format("\r\n\r\n{}\r\n", p.getContent()).getBytes(StandardCharsets.UTF_8).length;
    		}
    	}
    	len += sep.getBytes(StandardCharsets.UTF_8).length + 2;
    	return len;
	}
	
	public Bytes read() throws IOException {
		if(partsIndex >= parts.size()) {
			return null;
		}
		String sep = "--" + boundary;
		Bytes ret = new Bytes(CACHE_SIZE);
		int j = 0;
    	for(int i = partsIndex; i < parts.size(); i++) {
    		Multipart p = parts.get(i);
			if(j > 3) {
				break;
			}
			j++;
    		if(fileOff > 0) {
        		if(p.getFile() != null) {
                	int total = (int) (p.getFile().length() - fileOff);
                	if(total <= CACHE_SIZE) {
                    	byte[] buf = new byte[total];
                    	int read = 0;
                    	while((read = reader.read(buf)) >= 0) {
                    		if(read > 0) {
                        		ret.write(buf, 0, read);
                    		}
                    	}
                    	reader.close();
                    	fileOff = 0;
                	} else {
                    	byte[] buf = new byte[CACHE_SIZE];
                    	int off = 0, read = 0;
                    	while((read = reader.read(buf)) >= 0) {
                    		if(read > 0) {
                    			ret.write(buf, 0, read);
                    			off += read;
                    		}
                    		if(off >= CACHE_SIZE) {
                    			break ;
                    		}
                    	}
                    	fileOff += read;
            			partsIndex = i;
                    	return ret;
                	}
        		} else if(p.getInput() != null) {
                	byte[] buf = new byte[CACHE_SIZE];
                	int off = 0, read = 0;
                	while((read = reader.read(buf)) >= 0) {
                		if(read > 0) {
                			ret.write(buf, 0, read);
                			off += read;
                		}
                		if(off >= CACHE_SIZE) {
                			break ;
                		}
                	}
                	if(read >= 0) {
                    	fileOff += off;
            			partsIndex = i;
                    	return ret;
                	} else {
                		fileOff = 0;
                	}
        		}
    		} else {
        		ret.write(String.format("%s\r\n", sep).getBytes(StandardCharsets.UTF_8));
        		ret.write( String.format("Content-Disposition: form-data; name=\"%s\"", p.getName()).getBytes(StandardCharsets.UTF_8));
        		if(p.getType() == MultipartType.FILE) {
                	ret.write( String.format("; filename=\"%s\"\r\n", p.getFileName()).getBytes(StandardCharsets.UTF_8));
            		ret.write( String.format("Content-Type: %s\r\n\r\n", p.getContentType()).getBytes(StandardCharsets.UTF_8));
                    if(p.getContent() != null) {
                    	ret.write(p.getContent());
                    } else if(p.getFile() != null) {
                    	int needed = CACHE_SIZE - ret.available();
                    	long total = p.getFile().length();
                    	if(needed <= 0) {
                        	reader = new FileInputStream(p.getFile());
                			partsIndex = i;
                			return ret;
                    	} else {
                        	if(total <= needed) {
                        		ret.write(Files.readAllBytes(p.getFile().toPath()));
                        	} else {
                            	reader = new FileInputStream(p.getFile());
                            	byte[] buf = new byte[needed];
                            	int read = reader.read(buf);
                            	if(read > 0) {
                                	ret.write(Arrays.copyOfRange(buf, 0, read));
                                	fileOff = read;
                        			partsIndex = i;
                        			return ret;
                            	}
                        	}
                    	}
                    } else if(p.getInput() != null) {
                    	int needed = CACHE_SIZE - ret.available();
                    	byte[] buf = new byte[needed];
                    	int read = p.getInput().read(buf);
                    	if(read > 0) {
                    		ret.write(buf, 0, read);
                        	fileOff = read;
                			partsIndex = i;
                    		return ret;
                    	}
                    }
        		} else {
        			ret.write("\r\n\r\n".getBytes()); 
        			ret.write(p.getContent());
        		}
    		}
        	ret.write("\r\n".getBytes());
    		if (ret.available() >= CACHE_SIZE) {
    			partsIndex = i;
    			return ret;
    		}
    	}
    	partsIndex = parts.size();
    	ret.write( (sep + "--").getBytes());
    	return ret;
	}
}
