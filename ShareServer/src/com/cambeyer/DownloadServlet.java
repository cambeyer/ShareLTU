package com.cambeyer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import Decoder.BASE64Encoder;
 
public class DownloadServlet extends HttpServlet {
	
    private static final long serialVersionUID = 1L;
     
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {         
		String filename = (String) request.getAttribute("filename");
		String uuid = (String) request.getAttribute("uuid");
		
		String dirPath = getServletContext().getRealPath("") + File.separator + UploadServlet.UPLOAD_DIRECTORY + File.separator;
		
		FileManager.removeStaleFiles(dirPath);

		ArrayList<FileObject> candidates = FileManager.getFilesByToUUID(uuid);
		for (int i = 0; i < candidates.size(); i++)
		{
			if (candidates.get(i).filename.equals(filename))
			{
		        PrintWriter writer = response.getWriter();
		        writer.println(new BASE64Encoder().encode(IOUtils.toByteArray(new FileInputStream(dirPath + filename))).replace("\r", "").replace("\n", ""));
		        writer.flush();
			}
		}
    }
}