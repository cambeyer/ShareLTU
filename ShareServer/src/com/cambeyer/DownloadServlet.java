package com.cambeyer;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
 
public class DownloadServlet extends HttpServlet {
	
    private static final long serialVersionUID = 1L;
     
	@SuppressWarnings("rawtypes")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {         
		String filename = (String) request.getAttribute("filename");
		String fromuuid = (String) request.getAttribute("fromuuid");
		String touuid = (String) request.getAttribute("touuid");
        // constructs the directory path to fetch file from
        String filePath = getServletContext().getRealPath("") + File.separator + UploadServlet.UPLOAD_DIRECTORY;
        
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
        Servlet
        ServletFileUpload upload = new ServletFileUpload(factory);
        
    	String filename = "";
    	String fromuuid = "";
    	String touuid = "";
         
        try
        {
            // parses the request's content
            List formItems = upload.parseRequest(request);
            Iterator iter = formItems.iterator();
             
            // iterates over form's fields
            while (iter.hasNext())
            {
                FileItem item = (FileItem) iter.next();
                // processes only fields that are form fields
                if (item.isFormField())
                {
                	if (item.getFieldName().equals("fromuuid"))
                	{
                		fromuuid = item.getString();
                	}
                	else if (item.getFieldName().equals("touuid"))
                	{
                		touuid = item.getString();
                	}
                	else if (item.getFieldName().equals("filename"))
                	{
                		filename = item.getString();
                	}
                }
            }
        	//********************
        }
        catch (Exception ex)
        {
        }
    }
}