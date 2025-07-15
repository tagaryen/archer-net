package com.archer.net.http.multipart;

import java.nio.charset.StandardCharsets;

public class Multipart {
    private MultipartType type;

    private String name;

    private String fileName;

    private byte[] content;
    
    private String contentType;
    
    public Multipart() {}
    
    public Multipart(String name, String value) {
    	this(name, null, value.getBytes(StandardCharsets.UTF_8), null);
		this.type = MultipartType.TEXT;
    }
    


    public Multipart(String name, String fileName, byte[] content, String contentType) {
		this.type = MultipartType.FILE;
		this.name = name;
		this.fileName = fileName;
		this.content = content;
		this.contentType = contentType;
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
    }

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}
