import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

public class ReportGenerator {

    public static void main(String[] args) {
        
        System.out.print("Enter the input .xml file: ");
        Scanner scanner = new Scanner(System.in);
        String inputFileName = scanner.nextLine().trim();

        if (!inputFileName.toLowerCase().endsWith(".xml")) {
            System.err.println("Invalid format");
            return;
        }
        
        String workingDirectory = System.getProperty("user.dir");
        File inputFile = new File(workingDirectory, inputFileName);

        if (!inputFile.exists()) {
            System.err.println("File not found in working directory");
            return;
        }
        
        //Чтение XML
        try {
            //Чтение XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputFile);
            doc.getDocumentElement().normalize();
    
            Map<Integer, Map<String, String>> departments = new HashMap<>();
            NodeList depNodes = doc.getElementsByTagName("department");
            for (int i = 0; i < depNodes.getLength(); i++) {
                    Node nNode = depNodes.item(i);
                    Element el = (Element) nNode;
                    Map<String, String> dep = new HashMap<>();
                    dep.put("name", el.getAttribute("name"));
                    dep.put("label", el.getAttribute("label"));
                    int key = Integer.parseInt(el.getAttribute("key"));
                    departments.put(key, dep);
            }

            Map<Integer, Map<String, String>> users = new HashMap<>();
            NodeList userNodes = doc.getElementsByTagName("user");
            for (int i = 0; i < userNodes.getLength(); i++) {
                    Node nNode = userNodes.item(i);
                    Element el = (Element) nNode;
                    Map<String, String> user = new HashMap<>();
                    user.put("name", el.getAttribute("name"));
                    user.put("login", el.getAttribute("login"));
                    user.put("password", el.getAttribute("password"));
                    user.put("key_department", el.getAttribute("key_department"));
                    int key = Integer.parseInt(el.getAttribute("key"));
                    users.put(key, user);
            }

            Map<Integer, Map<String, String>> sessions = new HashMap<>();
            NodeList sessionsNodes = doc.getElementsByTagName("session");
            for (int i = 0; i < sessionsNodes.getLength(); i++) {
                    Node nNode = sessionsNodes.item(i);
                    Element el = (Element) nNode;
                    Map<String, String> session = new HashMap<>();
                    session.put("key_user", el.getAttribute("key_user"));
                    session.put("date_logon", el.getAttribute("date_logon"));
                    session.put("date_logoff", el.getAttribute("date_logoff"));
                    int key = Integer.parseInt(el.getAttribute("key"));
                    sessions.put(key, session);
            }

            //Готовлю отчет
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Map<Integer, int[]> report = new HashMap<>();
            Map<Integer, String> depNames = new HashMap<>();
            for (Map.Entry<Integer, Map<String, String>> sessionEntry : sessions.entrySet()) {
                Map<String, String> session = sessionEntry.getValue();

                int userId = Integer.parseInt(session.get("key_user"));
                Map<String, String> user = users.get(userId);

                int deptId = Integer.parseInt(user.get("key_department"));
                Map<String, String> dep = departments.get(deptId);
            
                LocalDateTime logon = LocalDateTime.parse(session.get("date_logon"), dtf);
                LocalDateTime logoff = LocalDateTime.parse(session.get("date_logoff"), dtf);
                long minutes = ChronoUnit.MINUTES.between(logon, logoff);

                int[] stat = report.get(deptId);
                if (stat == null) {
                    stat = new int[]{0, 0}; // [кол-во сессий, сумма минут]
                    report.put(deptId, stat);
                    depNames.put(deptId, dep.get("name"));
                }
                stat[0]++;          // количество сессий
                stat[1] += (int) minutes; // суммарное время
            }

            //Формирую XML
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<report>\n");

            for (Map.Entry<Integer, int[]> entry : report.entrySet()) {
                int keyDep = entry.getKey();
                int count = entry.getValue()[0];
                int totalMinutes = entry.getValue()[1];
                String name = depNames.get(keyDep);
                xml.append("  <department");
                xml.append(" key_department=\"").append(keyDep).append("\"");
                xml.append(" name_department=\"").append(name).append("\"");
                xml.append(" count_session=\"").append(count).append("\"");
                xml.append(" time_session=\"").append(totalMinutes).append("\"");
                xml.append("/>\n");
            }
            
            xml.append("</report>");
            Path outputPath = new File(workingDirectory, "report.xml").toPath();
            Files.writeString(outputPath, xml.toString());

        
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}