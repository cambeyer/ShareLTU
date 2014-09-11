package com.cambeyer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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
    
    private static final int THRESHOLD_SIZE = 1024 * 1024 * 3;  // 3 MB
    private static final int MAX_FILE_SIZE = 2000000000; // 2 GB
    private static final int MAX_REQUEST_SIZE = 2147483647; // 2.14748 GB
    
    private static final int DEFAULT_BUFFER_SIZE = 10240; // 10KB.
     
	@SuppressWarnings("rawtypes")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {         
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(THRESHOLD_SIZE);
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
         
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setFileSizeMax(MAX_FILE_SIZE);
        upload.setSizeMax(MAX_REQUEST_SIZE);
        
    	String filename = "";
    	String uuid = "";
         
        try
        {
            List formItems = upload.parseRequest(request);
            Iterator iter = formItems.iterator();
             
            // iterates over form's fields
            while (iter.hasNext())
            {
                FileItem item = (FileItem) iter.next();
                // processes only fields that are form fields
                if (item.isFormField())
                {
                	if (item.getFieldName().equals("uuid"))
                	{
                		uuid = item.getString();
                	}
                	else if (item.getFieldName().equals("filename"))
                	{
                		filename = item.getString();
                	}
                }
            }
        } catch (Exception ex)
        {
        }
		
		String dirPath = getServletContext().getRealPath("") + File.separator + UploadServlet.UPLOAD_DIRECTORY + File.separator;
		
		FileManager.removeStaleFiles(dirPath);
		
		ArrayList<FileObject> candidates = FileManager.getFilesByToUUID(uuid);
		for (int i = 0; i < candidates.size(); i++)
		{
			if (candidates.get(i).filename.equals(filename))
			{
		        // Decode the file name (might contain spaces and on) and prepare file object.
		        File file = new File(dirPath + filename);

		        // Init servlet response.
		        response.reset();
		        response.setBufferSize(DEFAULT_BUFFER_SIZE);
		        response.setContentType(candidates.get(i).type);
		        response.setHeader("Content-Length", String.valueOf(file.length()));
		        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

		        // Prepare streams.
		        BufferedInputStream input = null;
		        BufferedOutputStream output = null;

		        try {
		            // Open streams.
		            input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
		            output = new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE);

		            // Write file contents to response.
		            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		            int length;
		            while ((length = input.read(buffer)) > 0) {
		                output.write(buffer, 0, length);
		            }
		        } finally {
		            // Gently close streams.
		            output.close();
		            input.close();
		        }
			}
		}
    }
}