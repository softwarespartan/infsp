package com.infsp.FileClient;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/29/11
 * Time: 2:03 PM
 */

public class FileRequestXmlEncoder {

    private StringBuilder xmlString;

    private final String eol = System.getProperty("line.separator");
    private final String tab = "\t";

    public FileRequestXmlEncoder(){
        this.xmlString = new StringBuilder("<FileRequest>"+eol+eol);
    }

    public void putFile(String src){
        this.putFile(src,1);
    }

    public void putFile(String src, int replicate){

        this.xmlString.append(tab+"<file>"+eol);
        this.xmlString.append(tab+tab+"<mode>put</mode>"+eol);
        this.xmlString.append(tab+tab+"<src>"+src+"</src>"+eol);
        this.xmlString.append(tab+tab+"<replicate>"+replicate+"</replicate>"+eol);
        this.xmlString.append(tab+"</file>"+eol+eol);
    }

    public void getFile(String src,String dst){

        this.xmlString.append(tab+"<file>"+eol);
        this.xmlString.append(tab+tab+"<mode>get</mode>"+eol);
        this.xmlString.append(tab+tab+"<src>"+src+"</src>"+eol);
        this.xmlString.append(tab+tab+"<dst>"+dst+"</dst>"+eol);
        this.xmlString.append(tab+"</file>"+eol+eol);
    }

    public void delFile(String src){

        this.xmlString.append(tab+"<file>"+eol);
        this.xmlString.append(tab+tab+"<mode>del</mode>"+eol);
        this.xmlString.append(tab+tab+"<src>"+src+"</src>"+eol);
        this.xmlString.append(tab+"</file>"+eol+eol);
    }

    public String encode(){
        return this.xmlString.toString()+"</FileRequest>";
    }

    public void clear(){
        this.xmlString = new StringBuilder("<FileRequest>"+eol+eol);
    }

    public static void main(String[] args){
        FileRequestXmlEncoder encoder = new FileRequestXmlEncoder();

        encoder.putFile("/some/path/to/a/file");
        encoder.getFile("/a/file/on/the/local/disk","/path/to/nowhere");
        encoder.delFile("/youre/dead/file/fucking/dead");

        String request = encoder.encode();

        System.out.println(request);
    }

}
