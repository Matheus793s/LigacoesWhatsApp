package br.com.ligacoeswhatsapp;

/** Normalização conservadora: não inventa DDD. Números com + usam o DDI informado. */
public final class PhoneNumbers {
    private PhoneNumbers() { }

    public static String whatsappDigits(String raw) {
        if (raw == null) return null;
        String input = raw.trim();
        if (input.isEmpty()) return null;
        String digits = input.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;

        // Um número com + já informa seu código de país.
        if (input.startsWith("+") && digits.length() >= 8 && digits.length() <= 15) {
            return digits;
        }
        // 00 + código de país, por exemplo 005585999999999.
        if (digits.startsWith("00") && digits.length() >= 12 && digits.length() <= 17) {
            String international = digits.substring(2);
            return international.length() >= 8 && international.length() <= 15 ? international : null;
        }
        // Ligações brasileiras via operadora: 0 + XX (operadora) + DDD + número.
        if (digits.startsWith("0") && (digits.length() == 13 || digits.length() == 14)) {
            digits = digits.substring(3);
        } else if (digits.startsWith("0") && (digits.length() == 11 || digits.length() == 12)) {
            // 0 + DDD + número.
            digits = digits.substring(1);
        }

        // +55 sem símbolo +: DDI 55 + DDD (2) + telefone (8 ou 9).
        if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
            return digits;
        }
        // Número brasileiro com DDD, sem código de país.
        if (digits.length() == 10 || digits.length() == 11) {
            return "55" + digits;
        }
        // Sem DDD ou formato atípico: peça ao usuário para corrigir.
        return null;
    }
}
