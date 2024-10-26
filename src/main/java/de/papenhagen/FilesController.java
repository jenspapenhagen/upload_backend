package de.papenhagen;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.multipart.FileItem;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
@Path("files.html")
public class FilesController {

    private static final Logger LOG = Logger.getLogger(FilesController.class);

    private static final List<String> WHITELIST = List.of("pdf", "jpg", "jpeg");
    //20MB
    private static final int MAX_FILE_SIZE = 20_000_000;

    @ConfigProperty(name = "upload.path", defaultValue = "uploadfiles")
    String uploadPath;

    @Location("files.html")
    Template template;

    @Location("successUpload.html")
    Template successTemplate;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public TemplateInstance upload(final MultipartFormDataInput input) throws IOException {
        if (isNull(input)) {
            LOG.error("no input");
            return template.instance();
        }
        final Map<String, Collection<FormValue>> uploadForm = input.getValues();

        for (final FormValue inputPart : uploadForm.get("fileupload")) {
            final FileItem fileItem = inputPart.getFileItem();
            final long fileSize = fileItem.getFileSize();

            LOG.debug("filesize: " + fileSize + " Bytes");
            if (fileSize > MAX_FILE_SIZE) {
                LOG.error("FileSize to big");
                return template.instance();
            }

            final String fileName = validate(inputPart);
            if (isNull(fileName)) {
                LOG.error("validation failed");
                return template.instance();
            }

            //move the given file into the uploadFile folder
            final java.nio.file.Path currentWorkingDir = Paths.get("").toAbsolutePath();
            final java.nio.file.Path path = Paths.get(currentWorkingDir.toString(), uploadPath, fileName);

            try (final RandomAccessFile srcFile = new RandomAccessFile(path.toString(), "rw")) {
                final FileChannel rwChannel = srcFile.getChannel();
                final ByteBuffer writeBuffer = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
                writeBuffer.put(fileItem.getInputStream().readAllBytes());
                rwChannel.close();

                LOG.debug("success upload");
            } catch (IOException ex) {
                LOG.error("IOException on creating the file: " + ex.getLocalizedMessage());
                return template.instance();
            }

        }

        return successTemplate.instance();
    }

    @GET
    public TemplateInstance get() {
        return template.instance();
    }


    /**
     * This Validate Methode is checking against a whitelist.
     *
     * @param inputPart of the MuliUpload
     * @return the filename if everything is fine.
     */
    @Nullable
    private String validate(final FormValue inputPart) {
        final MultivaluedMap<String, String> headers = inputPart.getHeaders();

        final String fileName = getFileName(headers);
        LOG.debug("fileName: " + fileName);
        if (isNull(fileName)) {
            LOG.error("fileName is NULL");
            return null;
        }

        final String fileExtension = getFileExtension(fileName);
        LOG.debug("fileExtension: " + fileExtension);
        if (isNull(fileExtension)) {
            LOG.error("fileExtension is NULL");
            return null;
        }

        if (!WHITELIST.contains(fileExtension)) {
            LOG.error("File not allowed");
            return null;
        }

        return fileName;
    }

    /**
     * This method gives back the fileName.
     * <p>
     * header example
     * {
     * Content-Type=[image/png],
     * Content-Disposition=[form-data; name="file"; filename="filename.extension"]
     * }
     *
     * @param header of the requested multiPart.
     * @return the fullFile name as string.
     */
    @Nullable
    private String getFileName(final MultivaluedMap<String, String> header) {
        if (isNull(header) || header.isEmpty()) {
            return null;
        }
        final String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

        for (final String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                final String[] name = filename.split("=");
                return name[1].trim().replaceAll("\"", "");
            }
        }
        return null;
    }

    /**
     * getting the file extension.
     * "jpg"
     * _NOT_ ".jpg"
     *
     * @param filename to get the extension.
     * @return the file extension as string.
     */
    @Nullable
    private String getFileExtension(final String filename) {
        if (isNull(filename)) {
            return null;
        }
        final int dotIndex = filename.lastIndexOf(".");
        if (dotIndex >= 0) {
            return filename.substring(dotIndex + 1);
        }
        return null;
    }

}
