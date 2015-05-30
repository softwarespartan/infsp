package com.infsp.parsers;

import java.util.HashSet;
import java.util.Set;
import java.util.TooManyListenersException;

import com.infsp.lic.IPWorksLicense;

import com.infsp.vfs.VirtualFileSystem;
import ipworks.IPWorksException;
import ipworks.Xmlp;
import ipworks.XmlpCharactersEvent;
import ipworks.XmlpCommentEvent;
import ipworks.XmlpEndElementEvent;
import ipworks.XmlpEndPrefixMappingEvent;
import ipworks.XmlpErrorEvent;
import ipworks.XmlpEvalEntityEvent;
import ipworks.XmlpEventListener;
import ipworks.XmlpIgnorableWhitespaceEvent;
import ipworks.XmlpMetaEvent;
import ipworks.XmlpPIEvent;
import ipworks.XmlpSpecialSectionEvent;
import ipworks.XmlpStartElementEvent;
import ipworks.XmlpStartPrefixMappingEvent;

import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;


/**
 * Created by IntelliJ IDEA.
 * User: abelbrown
 * Date: 7/13/11
 * Time: 10:53 PM
 */
public class VfsXmlParser implements XmlpEventListener {

    private final Xmlp xmlParser = new Xmlp();
    private final String xmlFile;
	public Set<String> diskSet;
    public int maxXfer = 4;
    public String journalPath = null;

    private String currentElement = "";

    static final Logger LOGGER = Logger.getLogger(VfsXmlParser.class);

    public VfsXmlParser(String xmlFile) throws IPWorksException{

        // set the name of the file to parse
        this.xmlFile = xmlFile;

        // init disk set
        this.diskSet = new HashSet<String>();

        // set up default configurator for now
        //BasicConfigurator.configure();

        // we'll read everything
        LOGGER.setLevel(Level.ERROR);

        try {
            // do not care about "proper" xml
            this.xmlParser.setValidate(false);

            // set the lic so we can use the obj
		    this.xmlParser.setRuntimeLicense(IPWorksLicense.IPWorksLicense);

            // xml parser should notify THIS for events
			this.xmlParser.addXmlpEventListener(this);

            // parse the xml file
            //LOGGER.debug("Parsing file: "+this.xmlFile);
            this.xmlParser.parseFile(this.xmlFile);

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

    public void startElement(XmlpStartElementEvent e) {
        this.currentElement = e.element;
    }

    public void characters(XmlpCharactersEvent e) {

        String theString = new String(e.text);

        if (this.currentElement.equalsIgnoreCase("disk")){

            // add the disk to the disk set
            this.diskSet.add(theString);

            // let the logger know
            LOGGER.debug("parsed disk: "+ theString);

        } else if (this.currentElement.equalsIgnoreCase("maxXfer")){

            // might not be integer
            try{

                // parse to integer and set
                this.maxXfer = Integer.parseInt(theString);

                // let the log know
                LOGGER.debug("parsing maxXfer as " + this.maxXfer);

            } catch (NumberFormatException ex){
                // yell about it on the log
                LOGGER.error("Using default value "+this.maxXfer+", can not parse maxXfer in xml file: "+this.xmlFile);
            }
        } else if (this.currentElement.equalsIgnoreCase("journalPath")){
            this.journalPath = theString;
            LOGGER.debug("Setting journal path to: "+this.journalPath);
        }
    }

    public void comment(XmlpCommentEvent e) {
    }

    public void endElement(XmlpEndElementEvent e) {
    }

    public void endPrefixMapping(XmlpEndPrefixMappingEvent e) {
    }

    public void error(XmlpErrorEvent e) {
    }

    public void evalEntity(XmlpEvalEntityEvent e) {
    }

    public void ignorableWhitespace(XmlpIgnorableWhitespaceEvent e) {
    }

    public void meta(XmlpMetaEvent e) {
    }

    public void PI(XmlpPIEvent e) {
    }

    public void specialSection(XmlpSpecialSectionEvent e) {
    }

    public void startPrefixMapping(XmlpStartPrefixMappingEvent e) {
    }

    public static void main(String[] args) throws IPWorksException{
        VfsXmlParser parser = new VfsXmlParser("/Users/abelbrown/IdeaProjects/InfStoragePlatform/src/infsp.xml");

    }
}
