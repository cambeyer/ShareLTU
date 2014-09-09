package com.cambeyer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
    private static final String UPLOAD_DIRECTORY = "data";
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
                    String fileName = new File(item.getName()).getName();
                    String filePath = uploadPath + File.separator + fileName;
                    File storeFile = new File(filePath);
                     
                    // saves the file on disk
                    item.write(storeFile);
                }
            }
            request.setAttribute("message", "Upload has been done successfully!");

            Sender sender = new Sender(API_KEY);
            Message message = new Message.Builder().build();
            Result result = sender.send(message, "APA91bFnfPedJJ3UwlLQg4zed_zxOVxxm9y6E-4OeY-QLgmRjD6H-lvbHU_ZrXlX2nlvhK4Z5rgf4sNPQG7Nkl93IHJxmYXou9xN6SK2k_HcVlax7veMYSZ039q3WNspzSKybyznoB3TqeQiKQnQ96gHDhRG2s8aew", 5);
        }
        catch (Exception ex)
        {
            request.setAttribute("message", "There was an error: " + ex.getMessage());
        }
        getServletContext().getRequestDispatcher("/message.jsp").forward(request, response);
    }
}