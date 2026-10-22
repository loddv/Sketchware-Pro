package mod.loddv.dev.tools.offlineTranslator.translator;

import android.os.Parcel;
import android.os.Parcelable;

public class TranslationError implements Parcelable {

    private final ErrorType type;
    private final String language;
    private final String message;

    public TranslationError(ErrorType type, String language, String message) {
        this.type = type != null ? type : ErrorType.UNEXPECTED;
        this.language = language;
        this.message = message;
    }

    protected TranslationError(Parcel in) {
        if (in.readInt() != 0) {
            this.type = ErrorType.CREATOR.createFromParcel(in);
        } else {
            this.type = ErrorType.UNEXPECTED;
        }
        this.language = in.readString();
        this.message = in.readString();
    }

    public ErrorType getType() {
        return type;
    }

    public String getLanguage() {
        return language;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (type != null) {
            dest.writeInt(1);
            type.writeToParcel(dest, flags);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(language);
        dest.writeString(message);
    }

    public static final Creator<TranslationError> CREATOR = new Creator<TranslationError>() {
        @Override
        public TranslationError createFromParcel(Parcel in) {
            return new TranslationError(in);
        }

        @Override
        public TranslationError[] newArray(int size) {
            return new TranslationError[size];
        }
    };
}
