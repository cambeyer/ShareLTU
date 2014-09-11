package com.cambeyer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.android.gcm.server.*;
 
public class UploadServlet extends HttpServlet {
	
    private static final long serialVersionUID = 1L;
 
    private static final String API_KEY = "AIzaSyDRpMb1qmrGB4dZvGmlU_PqIOx6_ZJC9ok";
    public static final String UPLOAD_DIRECTORY = "data";
    private static final int THRESHOLD_SIZE = 1024 * 1024 * 3;  // 3 MB
    private static final int MAX_FILE_SIZE = 2000000000; // 2 GB
    private static final int MAX_REQUEST_SIZE = 2147483647; // 2.14748 GB
    
    @SuppressWarnings("rawtypes")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (!ServletFileUpload.isMultipartContent(request))
        {
            PrintWriter writer = response.getWriter();
            writer.println("Request does not contain upload data");
            writer.flush();
            return;
        }
         
        // configures upload settings
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(THRESHOLD_SIZE);
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
         
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setFileSizeMax(MAX_FILE_SIZE);
        upload.setSizeMax(MAX_REQUEST_SIZE);
         
        // constructs the directory path to store upload file
        String uploadPath = getServletContext().getRealPath("") + File.separator + UPLOAD_DIRECTORY;
        
        // creates the directory if it does not exist
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists())
        {
            uploadDir.mkdir();
        }
        
        //***************************
        UserObject user = new UserObject();
        user.name = "Cameron Beyer";
        user.uuid = "353918058381696";
        user.regid = "APA91bFnfPedJJ3UwlLQg4zed_zxOVxxm9y6E-4OeY-QLgmRjD6H-lvbHU_ZrXlX2nlvhK4Z5rgf4sNPQG7Nkl93IHJxmYXou9xN6SK2k_HcVlax7veMYSZ039q3WNspzSKybyznoB3TqeQiKQnQ96gHDhRG2s8aew";
        user.lat = "42.545032";
        user.lon = "-83.118824";
        UserManager.add(user);
        
        UserObject user2 = new UserObject();
        user2.name = "Adam Drotar";
        user2.uuid = "99000114946589";
        user2.regid = "APA91bFlQHqo4rJzYhelJj6ncl09g8j83dTVIIx7I4GZqbujv4b0szdbGfmPbXEYdsvEOkC-QyDQr0Cx3mPijHV_5nIRoP7mjr8IFCWwI2z_a8T-nZzWy5ri7SKRvuQCOHv6X7nf7zukqD7oIa_4QWKEkiEwc9Qv1Q";
        user2.lat = "42.545032";
        user2.lon = "-83.118824";
        UserManager.add(user2);
        
    	String filename = "";
    	String fromuuid = "";
    	String touuid = "";
    	String type = "";
         
        try
        {
            // parses the request's content to extract file data
            List formItems = upload.parseRequest(request);
            Iterator iter = formItems.iterator();
             
            // iterates over form's fields
            while (iter.hasNext())
            {
                FileItem item = (FileItem) iter.next();
                // processes only fields that are not form fields
                if (!item.isFormField())
                {
                    filename = new File(item.getName()).getName();
                    String filePath = uploadPath + File.separator + filename;
                    File storeFile = new File(filePath);
                     
                    // saves the file on disk
                    item.write(storeFile);
                }
                else
                {
                	if (item.getFieldName().equals("fromuuid"))
                	{
                		fromuuid = item.getString();
                	}
                	else if (item.getFieldName().equals("touuid"))
                	{
                		touuid = item.getString();
                	}
                	else if (item.getFieldName().equals("type"))
                	{
                		type = item.getString();
                	}
                }
            }
        	
            request.setAttribute("message", "Upload successful; from UUID: " + fromuuid + ", to UUIDs: " + touuid);

            FileObject file = new FileObject();
            file.filename = filename;
            file.fromuuid = fromuuid;
            file.touuid = touuid;
            file.type = type;
            file.timestamp = new Date();
            FileManager.add(file);
            
            String[] recipients = touuid.split(",");
            
            for (int i = 0; i < recipients.length; i++)
            {
	            Sender sender = new Sender(API_KEY);
	            Message message = new Message.Builder()
	            .timeToLive(60*60*24) // one day
	            .delayWhileIdle(false)
	            .addData("sendername", UserManager.getUserByUUID(fromuuid).name)
	            .addData("filename", filename)
	            .addData("type", type)
	            .build();
	            sender.send(message, UserManager.getUserByUUID(recipients[i]).regid, 5);
            }
        }
        catch (Exception ex)
        {
            request.setAttribute("message", "There was an error: " + ex.getMessage() + "; from UUID: " + fromuuid + ", to UUID: " + touuid + " (" + UserManager.getUserByUUID(touuid).regid + ")");
        }
        getServletContext().getRequestDispatcher("/message.jsp").forward(request, response);
    }
}