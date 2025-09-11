package com.archer.net.http.multipart;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.archer.net.http.MineTypes;

public class Multipart {
    private MultipartType type;

    private String name;

    private String fileName;

    private byte[] content;
    
    private InputStream input;

    private File file;
    
    private String contentType;
    
    public Multipart() {}
    
    public Multipart(String name, String value) {
    	this(name, null, value.getBytes(StandardCharsets.UTF_8));
		this.type = MultipartType.TEXT;
    }
    
    public Multipart(String name, String fileName, byte[] content) {
		this.type = MultipartType.FILE;
		this.name = name;
		this.fileName = fileName;
		this.content = content;
		this.contentType = MineTypes.getMineType(fileName);
	}

    public Multipart(String name, String fileName, InputStream input) {
		this.type = MultipartType.FILE;
		this.name = name;
		this.fileName = fileName;
		this.input = input;
		this.contentType = MineTypes.getMineType(fileName);;
	}
    

    public Multipart(String name, File file) throws FileNotFoundException {
    	if(!file.exists()) {
    		throw new FileNotFoundException(file.getName());
    	}
		this.type = MultipartType.FILE;
		this.name = name;
		this.fileName = file.getName();
		this.file = file;
		this.contentType = MineTypes.getMineType(file);
	}

	public MultipartType getType() {
        return type;
    }

    public void setType(MultipartType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
        this.file = null;
        this.input = null;
    }
    

	public InputStream getInput() {
		return input;
	}

	public void setInput(InputStream input) {
		this.input = input;
		this.content = null;
		this.file = null;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
		this.content = null;
		this.input = null;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
