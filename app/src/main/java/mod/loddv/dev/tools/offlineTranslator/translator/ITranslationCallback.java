package mod.loddv.dev.tools.offlineTranslator.translator;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ITranslationCallback extends IInterface {

    void onTranslationResult(String translatedText) throws RemoteException;

    void onTranslationError(TranslationError error) throws RemoteException;

    abstract class Stub extends Binder implements ITranslationCallback {

        private static final String DESCRIPTOR = "dev.davidv.translator.ITranslationCallback";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static ITranslationCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin instanceof ITranslationCallback) {
                return (ITranslationCallback) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= IBinder.FIRST_CALL_TRANSACTION && code <= IBinder.LAST_CALL_TRANSACTION) {
                data.enforceInterface(DESCRIPTOR);
            }
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_onTranslationResult: {
                    String _arg0 = data.readString();
                    this.onTranslationResult(_arg0);
                    return true;
                }
                case TRANSACTION_onTranslationError: {
                    TranslationError _arg0 = null;
                    if (data.readInt() != 0) {
                        _arg0 = TranslationError.CREATOR.createFromParcel(data);
                    }
                    this.onTranslationError(_arg0);
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }

        static final int TRANSACTION_onTranslationResult = IBinder.FIRST_CALL_TRANSACTION + 0;
        static final int TRANSACTION_onTranslationError = IBinder.FIRST_CALL_TRANSACTION + 1;

        private static class Proxy implements ITranslationCallback {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public void onTranslationResult(String translatedText) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    data.writeString(translatedText);
                    mRemote.transact(TRANSACTION_onTranslationResult, data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }

            @Override
            public void onTranslationError(TranslationError error) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    data.writeInterfaceToken(DESCRIPTOR);
                    if (error != null) {
                        data.writeInt(1);
                        error.writeToParcel(data, 0);
                    } else {
                        data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_onTranslationError, data, null, IBinder.FLAG_ONEWAY);
                } finally {
                    data.recycle();
                }
            }
        }
    }
}
