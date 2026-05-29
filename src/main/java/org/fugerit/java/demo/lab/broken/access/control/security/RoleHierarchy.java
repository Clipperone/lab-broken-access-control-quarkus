package org.fugerit.java.demo.lab.broken.access.control.security;

import java.util.Collection;

/**
 * Gerarchia dei ruoli applicativi: {@code guest < user < admin}.
 *
 * <p>
 * NOTA DIDATTICA: il controllo "ruolo ≥ X" richiede una <b>gerarchia</b>, non un semplice
 * set-membership. Questo helper la rende esplicita (livelli ordinati) e centralizzata, evitando di
 * disseminare confronti su stringhe nel codice. È usato dagli scenari di autorizzazione che richiedono
 * un ruolo minimo (es. documenti di ufficio visibili a chi ha ruolo pari o superiore all'owner).
 * </p>
 */
public final class RoleHierarchy {

    private RoleHierarchy() {
        // utility class
    }

    /** Livello del ruolo: più alto = più privilegi. Ruolo sconosciuto/nullo = 0. */
    public static int levelOf(String roleCode) {
        if (roleCode == null) {
            return 0;
        }
        return switch (roleCode) {
            case "admin" -> 3;
            case "user" -> 2;
            case "guest" -> 1;
            default -> 0;
        };
    }

    /** Livello massimo tra i ruoli posseduti dall'utente. */
    public static int highestLevel(Collection<String> userRoles) {
        int max = 0;
        if (userRoles != null) {
            for (String role : userRoles) {
                max = Math.max(max, levelOf(role));
            }
        }
        return max;
    }

    /**
     * @return {@code true} se l'utente possiede un ruolo di livello pari o superiore a quello richiesto.
     */
    public static boolean isAtLeast(Collection<String> userRoles, String requiredRoleCode) {
        return highestLevel(userRoles) >= levelOf(requiredRoleCode);
    }

    /** @return il codice del ruolo più alto posseduto ({@code admin}/{@code user}/{@code guest}), o {@code null}. */
    public static String highestRoleCode(Collection<String> userRoles) {
        return switch (highestLevel(userRoles)) {
            case 3 -> "admin";
            case 2 -> "user";
            case 1 -> "guest";
            default -> null;
        };
    }
}
