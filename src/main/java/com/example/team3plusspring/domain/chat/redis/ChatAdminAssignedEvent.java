package com.example.team3plusspring.domain.chat.redis;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChatAdminAssignedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long roomId;
    private Long assignedAdminId;
}
