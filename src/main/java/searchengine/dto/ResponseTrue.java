package searchengine.dto;

import lombok.*;

@Getter
@Setter
public class ResponseTrue {
    private String result;
    public ResponseTrue(String result) {
        this.result = result;
    }

}
