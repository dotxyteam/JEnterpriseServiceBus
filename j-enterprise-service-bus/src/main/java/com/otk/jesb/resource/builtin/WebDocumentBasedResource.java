package com.otk.jesb.resource.builtin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.otk.jesb.PotentialError;
import com.otk.jesb.resource.Resource;

public abstract class WebDocumentBasedResource extends Resource {

	public WebDocumentBasedResource() {
	}

	public WebDocumentBasedResource(String name) {
		super(name);
	}

	public interface Source {

		InputStream getInputStream() throws IOException;

		String extractFileName();

		URI toURI();

	}

	public static class FileSource implements Source {

		private File file;

		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new FileInputStream(file);
		}

		@Override
		public String extractFileName() {
			return file.getName();
		}

		@Override
		public URI toURI() {
			return file.toURI();
		}

	}

	public static class URLSource implements Source {

		private String urlSpecification;

		public String getUrlSpecification() {
			return urlSpecification;
		}

		public void setUrlSpecification(String urlSpecification) {
			this.urlSpecification = urlSpecification;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			try {
				return new URL(urlSpecification).openStream();
			} catch (MalformedURLException e) {
				throw new IOException(e);
			}
		}

		@Override
		public String extractFileName() {
			try {
				return new File(new URL(urlSpecification).toURI().getPath()).getName();
			} catch (MalformedURLException e) {
				throw new PotentialError(e);
			} catch (URISyntaxException e) {
				throw new PotentialError(e);
			}
		}

		@Override
		public URI toURI() {
			try {
				return new URL(urlSpecification).toURI();
			} catch (MalformedURLException e) {
				throw new PotentialError(e);
			} catch (URISyntaxException e) {
				throw new PotentialError(e);
			}
		}

	}

}