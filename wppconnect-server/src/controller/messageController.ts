
import { Request, Response } from 'express';

import { descreverErroSeguro, unlinkAsync } from '../util/functions';

function returnError(req: Request, res: Response, error: any) {
  req.logger.error(descreverErroSeguro(error));
  if (res.headersSent) {
    return;
  }
  res.status(500).json({
    status: 'Error',
    message: 'Erro ao enviar a mensagem.',
  });
}

async function returnSucess(res: any, data: any) {
  res.status(201).json({ status: 'success', response: data, mapper: 'return' });
}

export async function sendMessage(req: Request, res: Response) {
  const { phone, message } = req.body;

  const options = req.body.options || {};

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(await req.client.sendText(contato, message, options));
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    req.io.emit('mensagem-enviada', results);
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function editMessage(req: Request, res: Response) {
  const { id, newText } = req.body;

  const options = req.body.options || {};
  try {
    const edited = await (req.client as any).editMessage(id, newText, options);

    req.io.emit('edited-message', edited);
    returnSucess(res, edited);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendFile(req: Request, res: Response) {
  const {
    phone,
    path,
    base64,
    filename = 'file',
    message,
    caption,
    quotedMessageId,
  } = req.body;

  const options = req.body.options || {};

  if (!path && !req.file && !base64)
    res.status(401).send({
      message: 'Sending the file is mandatory',
    });

  const pathFile = path || base64 || req.file?.path;
  const msg = message || caption;

  try {
    const results: any = [];
    for (const contact of phone) {
      results.push(
        await req.client.sendFile(contact, pathFile, {
          filename: filename,
          caption: msg,
          quotedMsg: quotedMessageId,
          ...options,
        })
      );
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    if (req.file) await unlinkAsync(pathFile);
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendVoice(req: Request, res: Response) {
  const {
    phone,
    path,
    filename = 'Voice Audio',
    message,
    quotedMessageId,
  } = req.body;

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(
        await req.client.sendPtt(
          contato,
          path,
          filename,
          message,
          quotedMessageId
        )
      );
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendVoice64(req: Request, res: Response) {
  const { phone, base64Ptt, quotedMessageId } = req.body;

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(
        await req.client.sendPttFromBase64(
          contato,
          base64Ptt,
          'Voice Audio',
          '',
          quotedMessageId
        )
      );
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendLinkPreview(req: Request, res: Response) {
  const { phone, url, caption } = req.body;

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(
        await req.client.sendLinkPreview(`${contato}`, url, caption)
      );
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendLocation(req: Request, res: Response) {
  const { phone, lat, lng, title, address } = req.body;

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(
        await req.client.sendLocation(contato, {
          lat: lat,
          lng: lng,
          address: address,
          name: title,
        })
      );
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendButtons(req: Request, res: Response) {
  const { phone, message, options } = req.body;

  try {
    const results: any = [];

    for (const contact of phone) {
      results.push(await req.client.sendText(contact, message, options));
    }

    if (results.length === 0)
      return returnError(req, res, 'Error sending message with buttons');

    returnSucess(res, phone);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendListMessage(req: Request, res: Response) {
  const {
    phone,
    description = '',
    sections,
    buttonText = 'SELECIONE UMA OPÇÃO',
  } = req.body;

  try {
    const results: any = [];

    for (const contact of phone) {
      results.push(
        await req.client.sendListMessage(contact, {
          buttonText: buttonText,
          description: description,
          sections: sections,
        })
      );
    }

    if (results.length === 0)
      return returnError(req, res, 'Error sending list buttons');

    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendOrderMessage(req: Request, res: Response) {
  const { phone, items } = req.body;

  const options = req.body.options || {};

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(await req.client.sendOrderMessage(contato, items, options));
    }

    if (results.length === 0)
      res.status(400).json('Error sending order message');
    req.io.emit('mensagem-enviada', results);
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendPollMessage(req: Request, res: Response) {
  const { phone, name, choices, options } = req.body;

  try {
    const results: any = [];

    for (const contact of phone) {
      results.push(
        await req.client.sendPollMessage(contact, name, choices, options)
      );
    }

    if (results.length === 0)
      return returnError(req, res, 'Error sending poll message');

    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendStatusText(req: Request, res: Response) {
  const { message } = req.body;

  try {
    const results: any = [];
    results.push(await req.client.sendText('status@broadcast', message));

    if (results.length === 0) res.status(400).json('Error sending message');
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function replyMessage(req: Request, res: Response) {
  const { phone, message, messageId } = req.body;

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(await req.client.reply(contato, message, messageId));
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    req.io.emit('mensagem-enviada', { message: message, to: phone });
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendMentioned(req: Request, res: Response) {
  const { phone, message, mentioned } = req.body;

  try {
    let response;
    for (const contato of phone) {
      response = await req.client.sendMentioned(
        `${contato}`,
        message,
        mentioned
      );
    }

    res.status(201).json({ status: 'success', response: response });
  } catch (error) {
    req.logger.error(descreverErroSeguro(error));
    res.status(500).json({
      status: 'error',
      message: 'Error on send message mentioned',
    });
  }
}
export async function sendImageAsSticker(req: Request, res: Response) {
  const { phone, path } = req.body;

  if (!path && !req.file)
    res.status(401).send({
      message: 'Sending the file is mandatory',
    });

  const pathFile = path || req.file?.path;

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(await req.client.sendImageAsSticker(contato, pathFile));
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    if (req.file) await unlinkAsync(pathFile);
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}
export async function sendImageAsStickerGif(req: Request, res: Response) {
  const { phone, path } = req.body;

  if (!path && !req.file)
    res.status(401).send({
      message: 'Sending the file is mandatory',
    });

  const pathFile = path || req.file?.path;

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(await req.client.sendImageAsStickerGif(contato, pathFile));
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    if (req.file) await unlinkAsync(pathFile);
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}

export async function sendPixMessage(req: Request, res: Response) {

  const { phone, keyType, name, key, instructions } = req.body;

  const options = req.body.options || {};

  try {
    const results: any = [];
    for (const contato of phone) {
      results.push(
        await req.client.sendPixKey(
          contato,
          { keyType, name, key, instructions },
          options
        )
      );
    }

    if (results.length === 0) res.status(400).json('Error sending message');
    req.io.emit('mensagem-enviada', results);
    returnSucess(res, results);
  } catch (error) {
    returnError(req, res, error);
  }
}
