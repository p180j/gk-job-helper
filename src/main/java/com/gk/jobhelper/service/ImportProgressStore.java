package com.gk.jobhelper.service;
import com.gk.jobhelper.entity.ImportFile;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
@Component public class ImportProgressStore {
    private final Map<Long, ImportFile> tasks = new ConcurrentHashMap<>();
    public ImportFile start(Long id, int total) { ImportFile p = new ImportFile(); p.setId(id); p.setTotalRows(total); p.setStatus("IMPORTING"); p.setProcessedRows(0); p.setSuccessRows(0); p.setFailedRows(0); tasks.put(id,p); return p; }
    public void update(Long id, String status, int processed, int success, int failed, String error) { ImportFile p=tasks.get(id); if(p==null) p=start(id,0); p.setStatus(status); p.setProcessedRows(processed); p.setSuccessRows(success); p.setFailedRows(failed); p.setErrorMessage(error); }
    public ImportFile get(Long id) { return tasks.get(id); }
}
