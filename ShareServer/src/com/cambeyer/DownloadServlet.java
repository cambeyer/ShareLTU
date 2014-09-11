package com.cambeyer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.apache.commons.io.IOUtils;

import Decoder.BASE64Encoder;
 
public class DownloadServlet extends HttpServlet {
	
    private static final long serialVersionUID = 1L;
    
    private static final int THRESHOLD_SIZE = 1024 * 1024 * 3;  // 3 MB
    private static final int MAX_FILE_SIZE = 2000000000; // 2 GB
    private static final int MAX_REQUEST_SIZE = 2147483647; // 2.14748 GB
     
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
				String output = new BASE64Encoder().encode(IOUtils.toByteArray(new FileInputStream(dirPath + filename))).replace("\r", "").replace("\n", "");
		        PrintWriter writer = response.getWriter();
//				String tempout = "";
//				for (int j = 0; j < output.length(); j++)
//				{
//					tempout += output.charAt(j);
//					if (tempout.length() % 100 == 0 || j + 1 == output.length())
//					{
//						writer.print(tempout);
//						writer.flush();
//						tempout = "";
//					}
//				}
		        writer.print(output);
		        writer.flush();
			}
		}
    }
}