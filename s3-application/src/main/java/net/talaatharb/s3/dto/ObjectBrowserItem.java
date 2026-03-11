package net.talaatharb.s3.dto;

public record ObjectBrowserItem(String name, String objectKey, boolean folder, long size, int itemCount,
		int directChildCount) {

	public ObjectBrowserItem(String name, String objectKey, boolean folder, long size) {
		this(name, objectKey, folder, size, 0, 0);
	}
}