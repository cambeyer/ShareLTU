package com.cambeyer;

import java.io.File;
import java.io.IOException;
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
        
    	String filename = "";
    	
        FileObject file = new FileObject();
    	UserObject user = new UserObject();
         
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
                    file.filename = filename;
                    File storeFile = new File(filePath);
                     
                    // saves the file on disk
                    item.write(storeFile);
                }
                else
                {
                	if (item.getFieldName().equals("fromuuid"))
                	{
                		file.fromuuid = item.getString();
                		user.uuid = item.getString();
                	}
                	else if (item.getFieldName().equals("touuid"))
                	{
                		file.touuid = item.getString();
                	}
                	else if (item.getFieldName().equals("type"))
                	{
                		file.type = item.getString();
                	}
                	
                	
                	if (item.getFieldName().equals("model"))
                	{
                		user.model = item.getString();
                	}
                	else if (item.getFieldName().equals("regid"))
                	{
                		user.regid = item.getString();
                	}
                	else if (item.getFieldName().equals("uuid"))
                	{
                		user.uuid = item.getString();
                	}
                	else if (item.getFieldName().equals("name"))
                	{
                		user.name = item.getString();
                	}
                	else if (item.getFieldName().equals("lat"))
                	{
                		if (item.getString() != "")
                		{
                			user.lat = item.getString();
                		}
                	}
                	else if (item.getFieldName().equals("lon"))
                	{
                		if (item.getString() != "")
                		{
                			user.lon = item.getString();
                		}
                	}
                }
            }
            
            if (user.name != null && user.uuid != null && user.lat != null && user.lon != null)
            {
            	user.timestamp = new Date();
            	UserManager.add(user);
            	
            	UserManager.removeStaleUsers();
            	
            	request.setAttribute("message", UserManager.JSONify(UserManager.getUsersByRadius(user.lat, user.lon, 1609.34))); //1609.34 meters = 1 mile
            	
            	//********************** set real distance threshold
            }
            else
            {
            	request.setAttribute("message", "");
            }
            
            if (file.fromuuid != null && file.touuid != null && file.filename != null && file.type != null)
            {
	            file.timestamp = new Date();
	            FileManager.add(file);
            
	            String[] recipients = file.touuid.split(",");
	            
	            for (int i = 0; i < recipients.length; i++)
	            {
		            Sender sender = new Sender(API_KEY);
		            Message message = new Message.Builder()
		            .timeToLive(60*60*24) // one day
		            .delayWhileIdle(false)
		            .addData("sendername", UserManager.getUserByUUID(file.fromuuid).name)
		            .addData("filename", file.filename)
		            .addData("type", file.type)
		            .addData("fromuuid", file.fromuuid)
		            .build();
		            sender.send(message, UserManager.getUserByUUID(recipients[i]).regid, 5);
	            }
            }
        }
        catch (Exception ex)
        {
            request.setAttribute("message", "");
        }
        getServletContext().getRequestDispatcher("/message.jsp").forward(request, response);
    }
}