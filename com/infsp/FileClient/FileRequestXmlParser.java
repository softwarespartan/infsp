package com.infsp.FileClient;

import com.infsp.lic.IPWorksLicense;
import ipworks.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.TooManyListenersException;

/**
 * Created by IntelliJ IDEA.
 * User: abel
 * Date: 7/29/11
 * Time: 11:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileRequestXmlParser implements XmlpEventListener{

    // control variables
    private String mode;
    private String src;
    private String dst;
    private int replicaCount = 1;
    private String currentElement;

    // meta-data
    public int nPut = 0;
    public int nGet = 0;
    public int nDel = 0;

    // object that does all the work
    private final Xmlp xmlParser = new Xmlp();

    // where to put file requests
    private final FileRequestExecutor executor;

    // blab blab blab
    static final Logger LOGGER = Logger.getLogger(FileRequestXmlParser.class);

    public FileRequestXmlParser(FileRequestExecutor executor) throws IPWorksException{

        // set the logger level
        LOGGER.setLevel(Level.ERROR);

        // assign the executor service
        this.executor = executor;

        try {
            // do not care about "proper" xml
            this.xmlParser.setValidate(false);

            // set the lic so we can use the obj
		    this.xmlParser.setRuntimeLicense(IPWorksLicense.IPWorksLicense);

            // xml parser should notify THIS for events
			this.xmlParser.addXmlpEventListener(this);

		} catch (TooManyListenersException e) {

            // let the log know
			LOGGER.error(e.toString());

		} catch (IPWorksException e) {

            // blab about it
			LOGGER.error(e.toString());

            // can't swallow this one
            throw e;
		}
    }


    public void characters(XmlpCharactersEvent event) {

        String theString  = new String(event.text);

        if (this.currentElement.equalsIgnoreCase("mode")){
            this.mode = theString;
        } else if (this.currentElement.equalsIgnoreCase("src")){
            this.src = theString;
        } else if (this.currentElement.equalsIgnoreCase("dst")){
            this.dst = theString;
        } else if (this.currentElement.equalsIgnoreCase("replicate")){
            try{
                this.replicaCount = Integer.parseInt(theString);
            } catch (NumberFormatException nfe){
                this.replicaCount = 1;
            }
        } else {
            // no op
        }
    }

    public void comment(XmlpCommentEvent event) {
    }

    public void endElement(XmlpEndElementEvent event) {

        String endElement = event.element;


        // only ack on </file>
        if (endElement.equalsIgnoreCase("file")){

            if (mode == null){
                LOGGER.error("no mode for file request");
                return;
            }

            // make sure we have enought for each mode
            if (this.mode.equalsIgnoreCase("put") &&
                    this.src != null){

                // add new putfilereq to the request set
                this.executor.execute(new PutFileRequest(this.src, this.replicaCount));

                // blab about it on the log
                LOGGER.debug("parsed put request x "+this.replicaCount+ " for file "+this.src);

                // update put count
                this.nPut = this.nPut + 1;

            } else if (this.mode.equalsIgnoreCase("get")
                        && this.src != null
                            && this.dst !=null){

                // add new get file request to the request set
                this.executor.execute(new GetFileRequest(this.src,this.dst));

                // let voyers know it's done
                LOGGER.debug("parsed get request: "+ this.src+" --> "+this.dst);

                // update number of gets
                this.nGet = this.nGet + 1;

            } else if (this.mode.equalsIgnoreCase("del")
                        && this.src != null){

                // oh well now ... delete what?
                this.executor.execute(new DelFileRequest(this.src));

                // stop watching me!
                LOGGER.debug("parsed del request: "+this.src);

                // update number of files to delete
                this.nDel = this.nDel + 1;

            } else {
                LOGGER.error("invalid file request");
            }

            //reset control variables
            this.mode = null;
            this.src  = null;
            this.dst  = null;
            this.replicaCount = 1;

        }
    }

    public void endPrefixMapping(XmlpEndPrefixMappingEvent event) {
    }

    public void error(XmlpErrorEvent event) {
    }

    public void evalEntity(XmlpEvalEntityEvent event) {
    }

    public void ignorableWhitespace(XmlpIgnorableWhitespaceEvent event) {
    }

    public void meta(XmlpMetaEvent event) {
    }

    public void PI(XmlpPIEvent event) {
    }

    public void specialSection(XmlpSpecialSectionEvent event) {
    }

    public void startElement(XmlpStartElementEvent event) {
        this.currentElement =  event.element;
    }

    public void startPrefixMapping(XmlpStartPrefixMappingEvent event) {
    }

    public String parse(String xmlData)throws IPWorksException{
        this.xmlParser.input(xmlData);
        return "Parsed "+this.nPut+" put, "+this.nGet+" get, "+ this.nDel + " deletes";
    }

    public void reset(){

        nPut = 0;
        nDel = 0;
        nGet = 0;

        this.mode = null;
        this.src  = null;
        this.dst  = null;
        this.replicaCount = 1;

        //this.xmlParser.reset();
    }

    public static void main(String[] args) throws IPWorksException{

//        BasicConfigurator.configure();
//
//        FileRequestXmlParser frxmlp = new FileRequestXmlParser();
//
//        String xmlString;
//
//        xmlString = "<fileRequest><file><mode>put</mode><src>/some/file</src><replicate>4</replicate></file></fileRequest>";
//
//        System.out.println(frxmlp.parse(xmlString));


    }
}
