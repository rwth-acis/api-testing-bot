package i5.las2peer.services.apiTestingBot.chat;

import i5.las2peer.apiTestModel.BodyAssertionOperator;
import i5.las2peer.services.apiTestingBot.context.BodyAssertionType;
import i5.las2peer.services.apiTestingBot.context.TestModelingContext;
import io.swagger.v3.oas.models.parameters.PathParameter;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

import static i5.las2peer.services.apiTestingBot.chat.Messages.*;
import static i5.las2peer.services.apiTestingBot.context.BodyAssertionType.*;
import static i5.las2peer.services.apiTestingBot.context.BodyAssertionType.ALL_LIST_ENTRIES_FIELD_CHECK;

public class MessageHandlerUtil {

    /**
     * Handles the answer to a yes/no question.
     *
     * @param responseMessageSB StringBuilder used to append an error message (if necessary).
     * @param intent            Intent
     * @param yes               Function that should be called if intent is "yes".
     * @param no                Function that should be called if intent is "no".
     * @return Whether the next state should be handled too.
     */
    public static boolean handleYesNoQuestion(StringBuilder responseMessageSB, String intent, BooleanSupplier yes, BooleanSupplier no) {
        if (intent.equals(Intent.YES)) {
            return yes.getAsBoolean();
        } else if (intent.equals(Intent.NO)) {
            return no.getAsBoolean();
        } else {
            responseMessageSB.append(ERROR_COULD_NOT_UNDERSTAND);
            return false;
        }
    }

    /**
     * Handles the answer to a question that allows to select an item from a numbered list of choices.
     * Checks if the entered number is in range [1,max]. If this is the case, handleSelectedNumber is called.
     * Otherwise, an error message is appended to the given StringBuilder.
     *
     * @param responseMessageSB    StringBuilder used to append error messages (if necessary).
     * @param message              Message entered by the user.
     * @param max                  Number of possible choices.
     * @param handleSelectedNumber Function that gets called if user entered a number in range [1,max].
     * @return True if an error occurred, e.g., if the user entered no number or a number which is not in range [1,max]. False otherwise.
     */
    public static boolean handleNumberSelectionQuestion(StringBuilder responseMessageSB, String message, int max,
                                                        IntConsumer handleSelectedNumber) {
        boolean error = false;
        try {
            int num = Integer.parseInt(message);
            int min = 1;
            if (min <= num && num <= max) {
                handleSelectedNumber.accept(num);
            } else {
                error = true;
                responseMessageSB.append(ENTER_NUMBER_BETWEEN(min, max));
            }
        } catch (NumberFormatException e) {
            error = true;
            responseMessageSB.append(ENTER_NUMBER);
        }
        return error;
    }

    /**
     * Appends the given lines to the StringBuilder and inserts a line break after each line.
     *
     * @param responseMessageSB StringBuilder
     * @param lines             Lines that should be appended.
     */
    public static void appendLinesWithBreaks(StringBuilder responseMessageSB, String... lines) {
        for (String line : lines) {
            responseMessageSB.append(line);
            responseMessageSB.append("\n");
        }
    }

    /**
     * Returns the BodyAssertionOperator input type for the given intent.
     *
     * @param intent Intent
     * @return BodyAssertionOperator input type for the given intent.
     */
    public static int intentToInputType(String intent) {
        switch (intent) {
            case "type_jsonobject":
                return 2;
            case "type_jsonarray":
                return 3;
            case "type_string":
                return 4;
            case "type_number":
                return 5;
            case "type_boolean":
                return 6;
            default:
                return -1;
        }
    }

    public static BodyAssertionType numberToBodyAssertionType(int num) {
        switch (num) {
            case 1:
                return TYPE_CHECK;
            case 2:
                return FIELD_CHECK;
            case 3:
                return ANY_LIST_ENTRY_TYPE_CHECK;
            case 4:
                return ANY_LIST_ENTRY_FIELD_CHECK;
            case 5:
                return ALL_LIST_ENTRIES_TYPE_CHECK;
            case 6:
                return ALL_LIST_ENTRIES_FIELD_CHECK;
            default:
                return TYPE_CHECK;
        }
    }

    public static BodyAssertionOperator createBodyAssertionOperatorTypeCheck(BodyAssertionType assertionType,
                                                                             BodyAssertionOperator basicOperator) {
        if (assertionType == TYPE_CHECK) return basicOperator;
        else if (assertionType == ANY_LIST_ENTRY_TYPE_CHECK) return new BodyAssertionOperator(2, 0, basicOperator);
        else return new BodyAssertionOperator(3, 0, basicOperator);
    }

    public static BodyAssertionOperator createBodyAssertionOperatorFieldCheck(BodyAssertionType assertionType,
                                                                              BodyAssertionOperator basicOperator) {
        if (assertionType == FIELD_CHECK) return basicOperator;
        else if (assertionType == ANY_LIST_ENTRY_FIELD_CHECK) return new BodyAssertionOperator(2, 0, basicOperator);
        else return new BodyAssertionOperator(3, 0, basicOperator);
    }

    public static PathParameter getFirstUnsetPathParam(TestModelingContext context) {
        for (PathParameter param : context.getPathParams()) {
            String paramName = param.getName();
            // check if param is unset
            if (context.getPathParamValues() == null || !context.getPathParamValues().containsKey(paramName)) {
                return param;
            }
        }
        return null;
    }
}
