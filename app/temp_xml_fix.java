import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XmlFixer {
    public static void main(String[] args) {
        try {
            // Set up a validating parser
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            // Parse the file to check for errors
            File xmlFile = new File("app/src/main/res/layout/fragment_trading_detail.xml");
            Document doc = builder.parse(xmlFile);
            System.out.println("XML is well-formed");
            
        } catch (Exception e) {
            System.out.println("XML Error: " + e.getMessage());
        }
    }
}
