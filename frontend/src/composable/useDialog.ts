import { useDialog, useMessage } from 'naive-ui'

export function useDialogHelper() {
    const dialog = useDialog()
    const message = useMessage()

    const confirm = (title: string, content: string, onOk: () => void) => {
        dialog.warning({
            title,
            content,
            positiveText: '确认',
            negativeText: '取消',
            onPositiveClick: onOk
        })
    }

    const info = (msg: string) => {
        message.info(msg)
    }

    const success = (msg: string) => {
        message.success(msg)
    }

    const error = (msg: string) => {
        message.error(msg)
    }

    return {
        confirm,
        info,
        success,
        error
    }
}
