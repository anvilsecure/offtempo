package burp.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HttpRequestWithTimestamp {
    final int burpMessageId;
    final long sendNanoTime;

}
