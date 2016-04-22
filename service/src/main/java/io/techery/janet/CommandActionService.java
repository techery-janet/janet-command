package io.techery.janet;

import io.techery.janet.command.annotations.CommandAction;
import io.techery.janet.command.exception.CommandServiceException;

/**
 * For invoking custom logic as a command. {@linkplain CommandActionService} performs actions executing with a help of
 * annotation {@linkplain CommandAction @CommandAction}. Also to create command action it's necessary to implement
 * the interface {@linkplain CommandActionBase}. It contains the command's methods for running and cancellation.
 * To get command result use method {@linkplain CommandActionBase#getResult()}
 */
final public class CommandActionService extends ActionService {

    @Override protected Class getSupportedAnnotationType() {
        return CommandAction.class;
    }

    @SuppressWarnings("unchecked")
    @Override protected <A> void sendInternal(ActionHolder<A> holder) throws CommandServiceException {
        callback.onStart(holder);
        CommandActionBase action = checkAndCast(holder.action());
        if (action.isCanceled()) {
            return;
        }
        try {
            action.run(new ActionProgressInvoker((ActionHolder<CommandActionBase>) holder, callback));
        } catch (Throwable t) {
            if (action.isCanceled()) return;
            throw new CommandServiceException(action, t);
        }
    }

    @Override protected <A> void cancel(ActionHolder<A> holder) {
        CommandActionBase action = checkAndCast(holder.action());
        action.cancel();
        action.setCanceled(true);
    }

    private static CommandActionBase checkAndCast(Object action) {
        if (!(action instanceof CommandActionBase)) {
            throw new JanetInternalException(String.format("%s must extend %s", action.getClass()
                    .getCanonicalName(), CommandActionBase.class.getCanonicalName()));
        }
        return (CommandActionBase) action;
    }

    private static class ActionProgressInvoker implements CommandActionBase.CommandCallback {

        private final ActionHolder<CommandActionBase> holder;
        private final Callback callback;

        private ActionProgressInvoker(ActionHolder<CommandActionBase> holder, Callback callback) {
            this.holder = holder;
            this.callback = callback;
        }

        @Override public void onProgress(int progress) {
            callback.onProgress(holder, progress);
        }

        @Override public void onSuccess(Object result) {
            if (!holder.action().isCanceled()) {
                holder.action().setResult(result);
                callback.onSuccess(holder);
            }
        }

        @Override public void onFail(Throwable throwable) {
            if (!holder.action().isCanceled()) {
                callback.onFail(holder, new CommandServiceException(holder.action(), throwable));
            }
        }
    }
}
