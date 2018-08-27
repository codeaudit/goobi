package de.sub.goobi.metadaten;

/***************************************************************
 * Copyright notice
 *
 * (c) 2015 Robert Sehr <robert.sehr@intranda.com>
 *
 * All rights reserved
 *
 * This file is part of the Goobi project. The Goobi project is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * The GNU General Public License can be found at
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * This script is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * This copyright notice MUST APPEAR in all copies of this file!
 ***************************************************************/

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.goobi.beans.Process;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.HelperForm;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import lombok.Data;

public @Data class Image {

    private static final Logger logger = Logger.getLogger(Image.class);

    private static final int LARGE_THUMBNAIL_SIZE_FACTOR = 3;
    private static final String PLACEHOLDER_URL_3D = "/uii/template/img/goobi_3d_object_placeholder_large.png?version=1";
    private static final String PLACEHOLDER_URL_VIDEO = "/uii/template/img/goobi_placeholder_video_large.png?version=1";
    private static final String PLACEHOLDER_URL_AUDIO = "/uii/template/img/goobi_placeholder_audio_large.png?version=1";
    private static final String PLACEHOLDER_URL_NOTFOUND = "/uii/template/img/goobi_placeholder_notFound_large.png?version=1";
    private static final String PLACEHOLDER_URL_DEFAULT = "uii/template/img/thumbnail-placeholder.png?version=1";

    /**
     * The image format of the thumbnail urls. 'jpeg' per default
     */
    private String thumbnailFormat = "jpeg";
    /**
     * The image format for the main image urls contained in imageLevels
     * 
     * @deprecated imageLevels are deprecated, and iiif does its own format handling
     */
    @Deprecated
    private String largeImageFormat = "jpeg";
    /**
     * The image filename
     */
    private String imageName;
    /**
     * The full path to the image
     */
    private Path imagePath;
    /**
     * The order of the image within the images folder
     */
    private int order;
    /**
     * Thumbnail url of the default thumbnail size (usually the size given by 'MetsEditorThumbnailsize' in Goobi config
     */
    private String thumbnailUrl;
    /**
     * Thumbnail url for an enlarged thumbnail
     */
    private String largeThumbnailUrl;
    /**
     * Url to object information. If this is an actual image, the base iiif url is returned
     */
    private String objectUrl;

    /**
     * Url and size information for different resolutions of this image. Used to create layer urls for pyramid image display
     * 
     * @deprecated not required if using iiif for image display
     */
    @Deprecated
    private List<ImageLevel> imageLevels = null;
    /**
     * Url for bookmarking this image in the metsEditor
     */
    private String bookmarkUrl;
    /**
     * Tooltip to show when hovering over this image. The image filename per default
     */
    private String tooltip;
    /**
     * Size of this image as determined by the ContentServer from image metadata
     * 
     * @deprecated not required if using iiif for image display
     */
    @Deprecated
    private Dimension size = null;
    /**
     * The media type of this object. Usually 'image', but can also be 'object' or 'x3dom' for 3D objects
     */
    private Type type;

    /**
     * Creates an image object from the given file within the goobi-metadata hierarchy. All required urls are either set in the constructor, or
     * created when needed This class can also contain other media objects link 3D obects, video and audio. In these cases a default image is used for
     * thumbnails.
     * 
     * The most important call method is {@link #getObjectUrl()} which returns a IIIF image info object url for images (which can be directly passed
     * to an OpenSeadragon view) and a custom info object for 3D objects containing all information for the goobi javascript ObjectView to display the
     * object. For audio and videos, the thumbnails placeholder is returned
     * 
     * For thumbnail display, use #{@link #getThumbnailUrl()} and for zoomed thumbnails {@link #getLargeThumbnailUrl()}
     * 
     * TODO: implement {@link #getObjectUrl()} for audio/video to deliver the appropriate file/information
     * 
     * @param process The goobi process containing the image
     * @param imageFolderName The name of the image folder (typically ..._media or master_..._media)
     * @param filename The filename of the image file
     * @param order The order of the image within the goobi process
     * @param thumbnailSize The size of the thumbnails to create. May be null, in which case the configured default is used
     * @throws IOException If the image file could not be read
     * @throws InterruptedException From {@link Process#getImagesDirectory()}
     * @throws SwapException From {@link Process#getImagesDirectory()}
     * @throws DAOException From {@link Process#getImagesDirectory()}
     */
    public Image(Process process, String imageFolderName, String filename, int order, Integer thumbnailSize)
            throws IOException, InterruptedException, SwapException, DAOException {
        imageFolderName = Paths.get(imageFolderName).getFileName().toString();
        this.imagePath = getImagePath(process, imageFolderName, filename);
        this.imageName = this.imagePath.getFileName().toString();
        this.type = Type.getFromPath(imagePath);
        this.order = order;
        this.tooltip = filename;
        if (!Files.exists(this.imagePath)) {
            this.objectUrl = new HelperForm().getServletPathWithHostAsUrl() + PLACEHOLDER_URL_NOTFOUND;
        } else if (Type.image.equals(this.type)) {
            this.bookmarkUrl = createThumbnailUrl(this.imagePath, 1000, getThumbnailFormat(), "");
            this.objectUrl = createIIIFUrl(process, imageFolderName, filename);
        } else if (Type.object.equals(this.type) || Type.x3dom.equals(this.type)) {
            this.objectUrl = create3DObjectUrl(process, imageFolderName, filename);
        } else if (Type.unknown.equals(this.type)) {
            this.objectUrl = new HelperForm().getServletPathWithHostAsUrl() + PLACEHOLDER_URL_NOTFOUND;
        } else if (Type.audio.equals(this.type)) {
            this.objectUrl = new HelperForm().getServletPathWithHostAsUrl() + PLACEHOLDER_URL_AUDIO;
        } else if (Type.video.equals(this.type)) {
            this.objectUrl = new HelperForm().getServletPathWithHostAsUrl() + PLACEHOLDER_URL_VIDEO;
        } else {
            throw new IOException("Filetype handling not implemented at " + this.imagePath);
        }
        createThumbnailUrls(thumbnailSize != null ? thumbnailSize : ConfigurationHelper.getInstance().getMetsEditorThumbnailSize());
    }

    /**
     * 
     * @param imageName The full image path
     * @param order The order of the image within the image folder
     * @param thumbnailUrl The thumbnail url
     * @param largeThumbnailUrl large thumbnail url
     * @param tooltip The tooltip to display
     * @deprecated Use {@link #Image(Process, String, String, int)} instead
     */
    @Deprecated
    public Image(String imageName, int order, String thumbnailUrl, String largeThumbnailUrl, String tooltip) {
        this.imageName = imageName;
        this.type = getType(imageName);
        this.order = order;
        this.thumbnailUrl = thumbnailUrl;
        this.largeThumbnailUrl = largeThumbnailUrl;
        this.tooltip = tooltip;
    }

    /**
     * Recreates the urls to the image thumbnails using the given size
     * 
     * @param size The size of the smaller thumbnails
     */
    public void createThumbnailUrls(int size) {
        if (!Files.exists(this.imagePath)) {
            this.thumbnailUrl = PLACEHOLDER_URL_NOTFOUND;
        } else if (Type.image.equals(this.type)) {
            this.thumbnailUrl = createThumbnailUrl(this.imagePath, size, thumbnailFormat, "");
            this.largeThumbnailUrl = createThumbnailUrl(this.imagePath, size * LARGE_THUMBNAIL_SIZE_FACTOR, thumbnailFormat, "");
        } else if (Type.object.equals(this.type) || Type.x3dom.equals(this.type)) {
            this.thumbnailUrl = PLACEHOLDER_URL_3D;
        } else if (Type.unknown.equals(this.type)) {
            this.thumbnailUrl = PLACEHOLDER_URL_NOTFOUND;
        } else if (Type.audio.equals(this.type)) {
            this.thumbnailUrl = PLACEHOLDER_URL_AUDIO;
        } else if (Type.video.equals(this.type)) {
            this.thumbnailUrl = PLACEHOLDER_URL_VIDEO;
        } else {
            throw new IllegalStateException("Filetype handling not implemented at " + this.imagePath);
        }
    }

    /**
     * Gets the media type of the given file from its file extension
     * 
     * @param filename
     * @return returns the media type of the given file from its file extension
     * 
     */
    private Type getType(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        return Type.getFromFilenameExtension(extension);
    }

    /**
     * Add a level to pyramid image display
     * 
     * @param imageUrl The url to call
     * @param size The size of this level
     * @deprecated Image levels are replaced by IIIF image delivery
     */
    @Deprecated
    public void addImageLevel(String imageUrl, int size) {

        Dimension dim = getSize();
        if(dim == null || dim.getHeight()*dim.getWidth() == 0) {
            dim = new Dimension(size, size);
        }
        
        double scale = size / (double) (Math.max(dim.height, dim.width));
        Dimension dim2 = new Dimension((int) (dim.width * scale), (int) (dim.height * scale));
        ImageLevel layer = new ImageLevel(imageUrl, dim2);
        imageLevels.add(layer);
    }

    /**
     * Returns true if any image levels were created, false otherwise
     * 
     * @return true if any image levels were created, false otherwise
     * @deprecated Image levels are replaced by IIIF image delivery
     */
    @Deprecated
    public boolean hasImageLevels() {
        return !getImageLevels().isEmpty();
    }

    /**
     * Returns a json object containing the image information as required by an OpenSeadragon viewer as String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{")
        .append("width : ")
        .append(this.size.width)
        .append(",")
        .append("height : ")
        .append(this.size.height)
        .append(",")
        .append("sizes : [");
        for (ImageLevel imageLevel : imageLevels) {
            sb.append(imageLevel.toString()).append(", ");
        }
        sb.append("]").append("}");
        return sb.toString();
    }

    /**
     * Gets the size of the image
     * 
     * @return The size of the images, or null if the size could not be determined
     * @deprecated Not needed if displaying image via iiif url
     */
    @Deprecated
    public Dimension getSize() {
        if (this.size == null) {
            try {
                this.size = getImageSize(getImagePath());
            } catch (ImageManagerException | FileNotFoundException e) {
                logger.error("Error reading image size of " + getImagePath(), e);
            }

        }
        return this.size;
    }

    /**
     * Returns the image levels with urls and sizes in different resolutions for image pyramid display
     * 
     * @return The image levels with urls and sizes in different resolutions for image pyramid display
     * @deprecated Use IIIF url instead which creates the levels when requested
     */
    @Deprecated
    public List<ImageLevel> getImageLevels() {
        if (this.imageLevels == null) {
            if (Type.image.equals(getType())) {
                try {
                    this.imageLevels =
                            createImageLevels(getImagePath(), getLargeImageFormat(), ConfigurationHelper.getInstance().getMetsEditorImageSizes());
                } catch (ImageManagerException | FileNotFoundException e) {
                    logger.error("Error creating image levels for " + getImagePath(), e);
                    this.imageLevels = new ArrayList<>();
                }
            } else {
                this.imageLevels = Collections.EMPTY_LIST;
            }
        }
        return this.imageLevels;
    }

    /**
     * Reads the size of an image file from its image header
     * 
     * @param path Path to image file
     * @return The width and height of the image
     * @throws ImageManagerException If an error occured while reading the image file
     * @throws FileNotFoundException if the image file could not be found
     */
    public static Dimension getImageSize(Path path) throws ImageManagerException, FileNotFoundException {
        if (path != null) {
            URI uri = path.toUri();
            try (ImageManager manager = new ImageManager(uri)) {
                return manager.getMyInterpreter().getImageSize();
            }
        } else {
            return null;
        }
    }

    /**
     * Creates a goobi rest api url to a json object containing the information about the 3D object (mostly associated files)
     * 
     * @param process
     * @param imageFolderName
     * @param imageName
     * @return A goobi rest api url
     */
    public static String create3DObjectUrl(Process process, String imageFolderName, String imageName) {
        String contextPath = new HelperForm().getServletPathWithHostAsUrl();
        String url = contextPath + "/api/view/object/" + process.getId() + "/" + Paths.get(imageFolderName).getFileName().toString() + "/" + imageName
                + "/info.json";
        return url;
    }

    /**
     * Creates a rest url to the iiif image information about this image
     * 
     * @param process The process containing the image
     * @param imageFolderName The name of the image folder used
     * @param filename The filename of the image
     * @return A goobi rest api iiif url
     */
    public static String createIIIFUrl(Process process, String imageFolderName, String filename) {
        StringBuilder sb = new StringBuilder(new HelperForm().getServletPathWithHostAsUrl());
        sb.append("/api/image/")
        .append(process.getId())
        .append("/")
        .append(getImageFolderShort(imageFolderName))
        .append("/")
        .append(filename)
        .append("/info.json");
        return sb.toString();
    }

    private static Path getImagePath(org.goobi.beans.Process process, String imageFolderName, String filename)
            throws IOException, InterruptedException, SwapException, DAOException {
        Path path = Paths.get(process.getImagesDirectory(), imageFolderName, filename);
        return path;
    }

    private static String getImageFolderShort(String folder) {
        String imageFolder = Paths.get(folder).getFileName().toString();
        if (imageFolder.startsWith("master_") || imageFolder.startsWith("orig_")) {
            return "master";
        } else if (imageFolder.contains("_")) {
            return imageFolder.substring(imageFolder.lastIndexOf("_") + 1);
        } else {
            return imageFolder;
        }
    }

    /**
     * Creates a ContentServer url to a thumbnail of the given image file of the given size. If a base Url is provided, it is prepended to the
     * returned url, otherwise, the url starts with "/cs"
     * 
     * @param path The absolute path to the image File
     * @param size The resulting thumbnail will fit into a square with an edge length of the given size
     * @param baseUrl Preprended to the url which is otherwise relative to the Goobi application url
     * @return The thumbnail url as String
     */
    public static String createThumbnailUrl(Path path, int size, String format, String baseUrl) {

        StringBuilder url = new StringBuilder(baseUrl != null ? baseUrl : "");
        url.append("/cs").append("?action=").append("image").append("&format=").append(format).append("&sourcepath=");
        url.append(path.toUri());
        url.append("&width=").append(size).append("&height=").append(size);
        return url.toString().replaceAll("\\\\", "/");
    }

    /**
     * Creates a list of ImageLevels for pyramid views of this image
     * 
     * @param path The image file path
     * @param format The imge format in which to deliver the image
     * @param sizes A list of sizes in which to deliver the image
     * @return A list of {@link ImageLevel}s including the size and an url to this image delivered in that size
     * @throws ImageManagerException
     * @throws FileNotFoundException
     */
    public static List<ImageLevel> createImageLevels(Path path, String format, List<String> sizes)
            throws ImageManagerException, FileNotFoundException {
        String baseUrl = new HelperForm().getServletPathWithHostAsUrl();
        Dimension originalSize = getImageSize(path);
        List<ImageLevel> levels = new ArrayList<>();
        for (String sizeString : sizes) {
            try {
                int size = Integer.parseInt(sizeString);
                String imageUrl = createThumbnailUrl(path, size, format, baseUrl);
                double scale = size / (double) (Math.max(originalSize.height, originalSize.width));
                Dimension dim = new Dimension((int) (originalSize.width * scale), (int) (originalSize.height * scale));
                ImageLevel layer = new ImageLevel(imageUrl, dim);
                levels.add(layer);
            } catch (NullPointerException | NumberFormatException e) {
                logger.error("Cannot build image with size " + sizeString);
            }
        }
        return levels;
    }

    /**
     * Enum for media types to be handled
     * 
     * @author Florian Alpers
     *
     */
    public enum Type {
        image("jpg", "tif", "png", "jp2"),
        video("avi", "mpg4"),
        audio("ogg", "mp3", "wav"),
        object("obj", "stl", "ply", "fbx", "3ds", "gltf"),
        x3dom("x3d"),
        unknown("");

        private final List<String> extensions;

        private Type(String... extensions) {
            this.extensions = Arrays.asList(extensions);
        }

        /**
         * Determine the media type from the file extension of the given path
         * 
         * @param path
         * @return The corresponding type, {@link #unknown} if no media type could be determined
         */
        public static Type getFromPath(Path path) {
            String extension = FilenameUtils.getExtension(path.getFileName().toString());
            return getFromFilenameExtension(extension);
        }

        /**
         * Returns the corresponding type, {@link #unknown} if no media type could be determined
         * 
         * @param extension The file extension, without the preceding dot
         * @return The corresponding type, {@link #unknown} if no media type could be determined
         */
        public static Type getFromFilenameExtension(String extension) {
            for (Type type : Type.values()) {
                if (type.extensions.contains(extension.toLowerCase())) {
                    return type;
                }
            }
            return unknown;
        }
    }
}
